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

        // MOBI header starts 16 bytes into record 0
        var encoding = 'windows-1252';
        if (rec0 + 32 <= bytes.length) {
            var mobiId = String.fromCharCode(bytes[rec0 + 16], bytes[rec0 + 17],
                                             bytes[rec0 + 18], bytes[rec0 + 19]);
            if (mobiId === 'MOBI') {
                var encCode = readUint32BE(bytes, rec0 + 28);
                if (encCode === 65001) {
                    encoding = 'utf-8';
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
            if (compression === 2) {
                record = decompressPalmDOC(record);
            }
            parts.push(decoder.decode(record));
        }

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
