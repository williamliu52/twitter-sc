const express = require('express');
const bodyParser = require('body-parser');
const path = require('path');
const request = require('request');

const app = express();
const PORT = 5000;

// Object holding video URLs
let videos = {};

// Priority serve any static files.
app.use(express.static(path.resolve(__dirname, '../client/build')));
//Here we are configuring express to use body-parser as middle-ware.
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.post('/updateData', function (req, res) {
    console.log(req.body);
    res.sendStatus(200);
});

app.listen(PORT, function () {
    console.log(`Listening on port ${PORT}`);
});
