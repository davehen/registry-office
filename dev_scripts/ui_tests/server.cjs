// Static test server only. API responses are intercepted by the browser tests.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../src/main/resources/static');
const files = {
    '/': ['index.html', 'text/html'],
    '/index.html': ['index.html', 'text/html'],
    '/app.js': ['app.js', 'text/javascript'],
    '/style.css': ['style.css', 'text/css']
};
http.createServer((request, response) => {
    const entry = files[new URL(request.url, 'http://localhost').pathname];
    if (!entry) { response.writeHead(404).end(); return; }
    fs.readFile(path.join(root, entry[0]), (error, data) => {
        if (error) { response.writeHead(500).end(); return; }
        response.writeHead(200, { 'Content-Type': entry[1] }).end(data);
    });
}).listen(4173, '127.0.0.1');
