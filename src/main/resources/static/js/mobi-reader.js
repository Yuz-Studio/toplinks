/**
 * Minimal MOBI / PalmDOC reader for in-browser preview.
 * Supports PalmDOC compression (type 1 = none, type 2 = PalmDOC LZ).
 * Huffman-compressed MOBI (type 17480) is not supported.
 */
(function (global) {
    'use strict';

    function readUint32BE(bytes, off) {
        return ((bytes[off] << 24) | (bytes[off + 1] << 16) | (bytes[off + 2] << 8) | bytes[off + 3]) >>> 0;
    }

    function readUint16BE(bytes, off) {
        return ((bytes[off] << 8) | bytes[off + 1]) >>> 0;
    }

    function decompressPalmDOC(data) {
        var out = [];
        var i = 0;
        while (i < data.length) {
            var c = data[i++];
            if (c === 0) {
                out.push(0);
            } else if (c <= 8) {
                for (var j = 0; j < c && i < data.length; j++) {
                    out.push(data[i++]);
                }
            } else if (c <= 0x7F) {
                out.push(c);
            } else if (c <= 0xBF) {
                if (i >= data.length) break;
                var d2 = data[i++];
                var combined = (c << 8) | d2;
                var dist = (combined >> 3) & 0x7FF;
                var len = (combined & 7) + 3;
                for (var k = 0; k < len; k++) {
                    var pos = out.length - dist;
                    out.push(pos >= 0 ? out[pos] : 0);
                }
            } else {
                // 0xC0-0xFF: space + de-masked character
                out.push(0x20);
                out.push(c ^ 0x80);
            }
        }
        return new Uint8Array(out);
    }

    function detectEncoding(buffer, hasHighBytes) {
        var u8 = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);

        function utf8DecodableWithTrim(arr) {
            // Allow trimming up to 4 trailing bytes in case of cut multibyte at record boundary.
            for (var trim = 0; trim <= 4 && trim < arr.length; trim++) {
                var view = trim === 0 ? arr : arr.subarray(0, arr.length - trim);
                try {
                    new TextDecoder('utf-8', { fatal: true }).decode(view);
                    return true;
                } catch (e) {
                    // keep trying with more trim
                }
            }
            return false;
        }

        // BOM check first
        if (u8.length >= 3 && u8[0] === 0xEF && u8[1] === 0xBB && u8[2] === 0xBF) {
            return 'utf-8';
        }

        if (utf8DecodableWithTrim(u8)) {
            return 'utf-8';
        }

        // When we saw high bytes, prefer CJK encodings; avoid falling back to CP1252 unless everything else fails.
        var candidates = hasHighBytes ? ['gb18030', 'gbk', 'big5', 'windows-1252'] : ['windows-1252'];
        for (var idx = 0; idx < candidates.length; idx++) {
            try {
                new TextDecoder(candidates[idx], { fatal: true }).decode(u8);
                return candidates[idx];
            } catch (e) {
                // try next
            }
        }

        return 'utf-8';
    }

    function isDecodable(label, buffer) {
        var u8 = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
        // Allow trimming up to 4 trailing bytes to tolerate cut multibyte sequences
        // at record boundaries before rejecting an encoding.
        var maxTrim = label === 'utf-8' ? 4 : 0;
        for (var trim = 0; trim <= maxTrim; trim++) {
            try {
                var view = trim === 0 ? u8 : u8.subarray(0, u8.length - trim);
                new TextDecoder(label, { fatal: true }).decode(view);
                return true;
            } catch (e) {
                // try with one more trimmed byte
            }
        }
        return false;
    }
    
    function concatUint8(chunks, totalLength) {
        var out = new Uint8Array(totalLength);
        var offset = 0;
        for (var i = 0; i < chunks.length; i++) {
            out.set(chunks[i], offset);
            offset += chunks[i].length;
        }
        return out;
    }

    // Build a small sample (up to maxBytes) across text records to detect encoding.
    function buildSample(bytes, offsets, numRecords, textRecCount, compression, extraDataFlags, maxBytes) {
        var limit = maxBytes || 65536;
        var chunks = [];
        var total = 0;
        var sawHigh = false;
        var end = Math.min(textRecCount + 1, numRecords); // records are 1..textRecCount

        for (var r = 1; r < end; r++) {
            var start = offsets[r];
            var endOff = offsets[r + 1];
            if (start >= bytes.length || endOff > bytes.length || start >= endOff) {
                continue;
            }
            var record = bytes.slice(start, endOff);
            record = stripTrailingData(record, extraDataFlags);
            if (compression === 2) {
                record = decompressPalmDOC(record);
            }

            // Track if this record has any non-ASCII bytes.
            for (var i = 0; i < record.length; i++) {
                if (record[i] >= 0x80) {
                    sawHigh = true;
                    break;
                }
            }

            var remaining = limit - total;
            if (remaining <= 0) {
                break;
            }
            var take = Math.min(record.length, remaining);
            if (take > 0) {
                chunks.push(record.slice(0, take));
                total += take;
            }

            // Stop early if we have data and already saw high bytes.
            if (total >= limit || (sawHigh && total > 0)) {
                break;
            }
        }

        return {
            sample: total > 0 ? concatUint8(chunks, total) : new Uint8Array(0),
            sawHigh: sawHigh
        };
    }

    // Helper to strip MOBI trailing data based on extraDataFlags.
    // Reference: KindleUnpack specification.
    //
    // Bit 0 (0x01): "multibyte char overlap" — the last byte encodes in its low 2 bits
    //               how many bytes overlap with the next record. Strip those N overlap
    //               bytes + 1 (the size byte itself).
    //
    // Bits 1+ (0x02, 0x04, …): "trailing entry" — a variable-length integer at the
    //               current end of the record. Encoding: read backwards; each byte
    //               contributes 7 bits via (num = (num<<7)|(v&0x7f)), stop when a byte
    //               with high bit set is found. Strip value + number_of_size_bytes.
    function stripTrailingData(record, extraDataFlags) {
        if (!extraDataFlags) return record;

        // KindleUnpack getSizeOfTrailingDataEntry:
        // acc = (acc << 7) | (v & 0x7f) for each byte from end; stop when high bit set.
        // Returns value + numBytes (total bytes to remove from end).
        function getTrailingEntrySize(arr) {
            var num = 0;
            var numBytes = 0;
            for (var i = arr.length - 1; i >= 0; i--) {
                var v = arr[i];
                numBytes++;
                num = ((num << 7) | (v & 0x7f)) >>> 0;
                if (v & 0x80) {
                    break; // terminating byte
                }
            }
            return num + numBytes;
        }

        var arr = record;
        var flags = extraDataFlags;

        // Process higher bits (bit1, bit2, …) from highest down, skipping bit0.
        var highBits = [];
        for (var bit = 2; bit <= 0x8000; bit <<= 1) {
            if (flags & bit) { highBits.push(bit); }
        }
        for (var k = highBits.length - 1; k >= 0; k--) {
            var remove = getTrailingEntrySize(arr);
            if (remove > 0 && remove <= arr.length) {
                arr = arr.slice(0, arr.length - remove);
            }
        }

        // Handle bit 0 last: multibyte overlap.
        if (flags & 1) {
            if (arr.length >= 1) {
                var lastByte = arr[arr.length - 1];
                var overlapBytes = lastByte & 0x3;
                var totalStrip = overlapBytes + 1;
                if (totalStrip <= arr.length) {
                    arr = arr.slice(0, arr.length - totalStrip);
                }
            }
        }

        return arr;
    }

    function parseMobi(buffer) {
        var bytes = new Uint8Array(buffer);

        // Palm Database header: numRecords at offset 76 (2 bytes)
        var numRecords = readUint16BE(bytes, 76);

        // Record offsets start at byte 78; each entry is 8 bytes (4 = offset, 4 = attrs+uid)
        var offsets = [];
        for (var i = 0; i < numRecords; i++) {
            offsets.push(readUint32BE(bytes, 78 + i * 8));
        }
        offsets.push(bytes.length); // sentinel

        // Record 0 = PalmDOC header
        var rec0 = offsets[0];
        var compression  = readUint16BE(bytes, rec0);       // 1=none, 2=PalmDOC, 17480=Huffman
        var textRecCount = readUint16BE(bytes, rec0 + 8);   // number of text records
        var textRecSize  = readUint16BE(bytes, rec0 + 10);  // max size of a text record (usually 4096)

        // MOBI header starts 16 bytes into record 0
        var encoding = null;
        var extraDataFlags = 0;
        if (rec0 + 32 <= bytes.length) {
            var mobiId = String.fromCharCode(bytes[rec0 + 16], bytes[rec0 + 17],
                                             bytes[rec0 + 18], bytes[rec0 + 19]);
            if (mobiId === 'MOBI') {
                var encCode = readUint32BE(bytes, rec0 + 28);
                if (encCode === 65001) {
                    encoding = 'utf-8';
                } else if (encCode === 936) {
                    encoding = 'gbk';
                } else if (encCode === 950) {
                    encoding = 'big5';
                } else if (encCode === 54936) {
                    encoding = 'gb18030';
                }
                // NOTE: We intentionally ignore encCode === 1252 here.
                // Many Chinese MOBI files incorrectly state 1252 (CP1252) in the header.
                // By leaving encoding as null, we force the detection logic below to run,
                // which distinguishes between actual 1252 and GBK.
                
                // Extra data flags at MOBI header + 0xF2 (offset 242 from start of record 0)
                // MOBI header offset is 16, so 16 + 242 = 258
                if (rec0 + 258 + 2 <= bytes.length) {
                    extraDataFlags = readUint16BE(bytes, rec0 + 258);
                }
            }
        }

        if (compression === 17480) {
            return null; // Huffman not supported
        }

        console.log('[MobiReader] comp=' + compression + ' textRec=' + textRecCount + ' enc=' + (encoding || 'auto') + ' edf=' + extraDataFlags);

        // Only auto-detect when header gives no recognised encoding.
        if (!encoding) {
            var sampleInfo = buildSample(bytes, offsets, numRecords, textRecCount, compression, extraDataFlags, 65536);
            if (sampleInfo.sawHigh && sampleInfo.sample.length > 0) {
                encoding = detectEncoding(sampleInfo.sample, true);
            } else {
                encoding = 'utf-8';
            }
        }

        var decoder = new TextDecoder(encoding);
        var parts = [];
        for (var r = 1; r <= textRecCount && r < numRecords; r++) {
            var start = offsets[r];
            var end   = offsets[r + 1];
            var record = bytes.slice(start, end);
            
            // Remove extra bytes from the end of the record
            record = stripTrailingData(record, extraDataFlags);

            if (compression === 2) {
                record = decompressPalmDOC(record);
            }

            parts.push(decoder.decode(record, { stream: true }));
        }
        parts.push(decoder.decode()); // flush

        // Strip null characters that come from LZ back-references copying
        // null-padded regions in the compressed data. They don't break UTF-8
        // but show as garbage or invisible chars in rendered HTML.
        // eslint-disable-next-line no-control-regex
        return parts.join('').replace(/\x00/g, '');
    }

    function escapeHtml(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    global.MobiReader = {
        /**
         * Fetch a MOBI file from `url` and render its content into `container`.
         */
        preview: function (url, container) {
            container.innerHTML =
                '<div class="text-center py-4">' +
                '<div class="spinner-border text-secondary" role="status">' +
                '<span class="visually-hidden">Loading...</span></div></div>';

            fetch(url)
                .then(function (resp) {
                    if (!resp.ok) throw new Error('HTTP ' + resp.status);
                    return resp.arrayBuffer();
                })
                .then(function (buffer) {
                    var content = parseMobi(buffer);
                    if (content === null) {
                        container.innerHTML =
                            '<div class="alert alert-warning">This MOBI file uses Huffman compression and cannot be previewed online. Please download it and open with Kindle.</div>';
                        return;
                    }
                    var isHtml = /<(html|body|div|p|span|h[1-6]|br)/i.test(content);
                    if (isHtml) {
                        // Render HTML content in a sandboxed iframe via Blob URL
                        var blob = new Blob([content], { type: 'text/html; charset=utf-8' });
                        var blobUrl = URL.createObjectURL(blob);
                        var iframe = document.createElement('iframe');
                        iframe.setAttribute('sandbox', '');
                        iframe.style.cssText = 'width:100%;height:75vh;border:0;border-radius:4px;';
                        container.innerHTML = '';
                        container.appendChild(iframe);
                        iframe.src = blobUrl;
                    } else {
                        container.innerHTML =
                            '<pre style="max-height:75vh;overflow:auto;white-space:pre-wrap;word-break:break-word;">' +
                            escapeHtml(content) + '</pre>';
                    }
                })
                .catch(function (err) {
                    console.error(err);
                    container.innerHTML =
                        '<div class="alert alert-warning">Unable to preview this MOBI file: ' + escapeHtml(err.message) + '</div>';
                });
        }
    };
})(window);