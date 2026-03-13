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
            } else if (c < 0x80) {
                out.push(c);
            } else if (c < 0xC0) {
                var d = data[i++];
                var combined = (c << 8) | d;
                var dist = (combined >> 3) & 0x1FFF;
                var len = (combined & 7) + 3;
                for (var k = 0; k < len; k++) {
                    var pos = out.length - dist;
                    out.push(pos >= 0 ? out[pos] : 0);
                }
            } else {
                out.push(0x20); // space
                out.push(c ^ 0x80);
            }
        }
        return new Uint8Array(out);
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
        var encoding = 'windows-1252';
        var extraDataFlags = 0;
        if (rec0 + 32 <= bytes.length) {
            var mobiId = String.fromCharCode(bytes[rec0 + 16], bytes[rec0 + 17],
                                             bytes[rec0 + 18], bytes[rec0 + 19]);
            if (mobiId === 'MOBI') {
                var encCode = readUint32BE(bytes, rec0 + 28);
                if (encCode === 65001) {
                    encoding = 'utf-8';
                } else if (encCode === 1252) {
                    encoding = 'windows-1252';
                } else if (encCode === 936) {
                    encoding = 'gbk';
                }
                
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

        var decoder = new TextDecoder(encoding);
        var parts = [];
        for (var r = 1; r <= textRecCount && r < numRecords; r++) {
            var start = offsets[r];
            var end   = offsets[r + 1];
            var record = bytes.slice(start, end);
            
            // Remove extra bytes from the end of the record
            // MOBI files occasionally have 1-N bytes of extra data at the end of each text record.
            if (extraDataFlags > 0) {
                var extraSize = 0;
                var flags = extraDataFlags;
                // Multibyte (0x01): The last few bytes of the record may be part of a multibyte character.
                // However, in standard MOBI, this bit actually indicates that there is 'trailing data' 
                // and the last byte of the record (before other trailing data) tells you how many 
                // bytes to ignore.
                if (flags & 1) {
                    var n = 0;
                    var shift = 0;
                    var lenSize = 0;
                    // Read varlen integer from the end backwards
                    for (var lastIdx = record.length - 1; lastIdx >= 0; lastIdx--) {
                        var v = record[lastIdx];
                        lenSize++;

                        if (lastIdx === record.length - 1) {
                            // The very last byte must have the high bit set to be a valid end of a varlen sequence
                            if ((v & 0x80) === 0) {
                                // Invalid trailing len structure (or not present), abort simple strip?
                                // For safety, if we don't find the terminator, we might assume 0? 
                                // But usually bit 0 flag implies it is there.
                                // Let's try to proceed as if it's 0 if check fails, but usually it works.
                                lenSize = 0; // Abort
                                break;
                            }
                        }

                        if (v & 0x80) {
                            // High bit set. This is the end of a sequence (start of our backward read).
                            // If this is not the first byte we read, it means we hit the END of the PREVIOUS sequence
                            // (which is outside our scope), so we stop.
                            if (lastIdx < record.length - 1) {
                                lenSize--; // Don't include this byte
                                break;
                            }
                        } else {
                            // High bit clear. Continuation byte.
                        }
                        
                        n |= (v & 0x7f) << shift;
                        shift += 7;
                    }
                    // extraSize = data_length + length_of_size_field
                    extraSize += n + lenSize;
                }
                
                if (record.length > extraSize) {
                    record = record.slice(0, record.length - extraSize);
                }
            }

            if (compression === 2) {
                record = decompressPalmDOC(record);
            }
            parts.push(decoder.decode(record, { stream: true }));
        }
        parts.push(decoder.decode()); // flush

        return parts.join('');
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
                    container.innerHTML =
                        '<div class="alert alert-warning">Unable to preview this MOBI file: ' + escapeHtml(err.message) + '</div>';
                });
        }
    };
})(window);