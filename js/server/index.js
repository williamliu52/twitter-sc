const express = require('express');
const bodyParser = require('body-parser');
const path = require('path');
const request = require('request');

const app = express();
const PORT = process.env.PORT || 5000;

// Twitter status embed API
const EMBED = "https://publish.twitter.com/oembed";

// Object holding video URLs
let videos = [];

// Priority serve any static files.
app.use(express.static(path.resolve(__dirname, '../client/build')));
//Here we are configuring express to use body-parser as middle-ware.
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.post('/updateData', function (req, res) {
    let data = req.body.videos;
    for (var i=0; i<data.length; i++) {
        if (!videos.includes(data[i])) {
            videos.push(data[i]);
        }
    }
    res.sendStatus(200);
});

app.get('/getUrls', function (req, res) {
    let videoObj = { "videos" : videos };
    res.set('Content-Type', 'application/json');
    res.send(JSON.stringify(videoObj));
});

app.post('/getEmbed', function (req, res) {
    let vidUrl = req.body.url;
    // Since URLs are in video format need to get the original tweet URL
    request(vidUrl, function (error, resp, body) {
        if (!error) {
            let actualUrl = resp.request.href;
            // trim '/video/1' from end of URL
            actualUrl = actualUrl.substring(0, actualUrl.length-8);
            // Create request for Twitter embed API
            // Doc: https://dev.twitter.com/rest/reference/get/statuses/oembed
            let embedUrl = EMBED + '?url=' + actualUrl + '&hide_thread=true' + '&maxwidth=400';
            // Get embedded URL tweet
            request(embedUrl, function (error, resp, body) {
                try {
                    let result = JSON.parse(body);
                    let embedHtml = { "html" : result.html };
                    res.set('Content-Type', 'application/json');
                    res.send(JSON.stringify(embedHtml));
                } catch (e) {
                    console.log("Error stringifying: " + e);
                }
            })
        } else {
            console.log("Error getting embed: " + error);
        }
    })
});

app.listen(PORT, function () {
    console.log(`Listening on port ${PORT}`);
});
