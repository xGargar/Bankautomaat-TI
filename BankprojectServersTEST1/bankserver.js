const express = require('express');
const bodyParser = require('body-parser');

const expressPort = 8080;
const app = express();
app.use(bodyParser.json());

app.post('/api/getBalance', (req, res) => {
  console.log('Received data from Java client:', req.body);

  // Forward the data to endpoint /api/getBalance
  const http = require('http');
  const options = {
    host: '145.24.222.194',
    port: 8443,
    path: '/api/getBalance',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const httpReq = http.request(options, (httpRes) => {
    let responseData = '';

    httpRes.on('data', (chunk) => {
      responseData += chunk;
    });

    httpRes.on('end', () => {
      console.log('Received response from HTTP server:', responseData);
    });
  });

  httpReq.on('error', (error) => {
    console.error('Error sending data to HTTP server:', error);
  });

  console.log('Sending data to HTTP server:', req.body);
  httpReq.write(JSON.stringify(req.body));
  httpReq.end();

  res.send('Data received successfully');
});

app.listen(expressPort, () => {
  console.log(`Express server is running on port ${expressPort}`);
});
