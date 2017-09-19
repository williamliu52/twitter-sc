import React, {Component} from 'react';
import logo from './logo.svg';
import './App.css';
import { Button } from 'react-bootstrap';

class App extends Component {
    constructor(props) {
        super(props);
        this.state = {
            videos: [],
            embeds: []
        }
    }

    componentDidUpdate() {
        window.twttr.widgets.load()
    }

    getAllUrls = function() {
        fetch('/getUrls', {
            method: 'get'
        }).then(resp => {
            if (!resp.ok) {
                throw new Error(`status ${resp.status}`);
            }
            return resp.json();
        }).then(json => {
            this.setState({
                videos: json.videos || []
            });
        }).then(() => {
            for (let url of this.state.videos) {
                this.getEmbed(url);
            }
        });
    }

    getEmbed = function(url) {
        let param = {
            url: url
        };
        fetch('/getEmbed', {
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            method: 'post',
            body: JSON.stringify(param)
        }).then(resp => {
            if (!resp.ok) {
                throw new Error(`status ${resp.status}`);
            }
            return resp.json();
        }).then(json => {
            return json.html;
        }).then(html => {
            if (this.state.embeds.indexOf(html) === -1) {
                let newEmbeds = this.state.embeds.push(html);
                this.setState({
                    embed: newEmbeds
                });
            }
        });
    }

    render() {
        const embeddedTweets = this.state.embeds.map((embed, idx) => (
            <div key={idx} dangerouslySetInnerHTML={{__html: embed}}>
            </div>
        ));

        return (
            <div className="App">
                <div className="App-header">
                    <h2>TwitterSC</h2>
                </div>
                <Button bsSize="large" onClick={this.getAllUrls.bind(this)}>Refresh Videos</Button>
                {embeddedTweets}
            </div>
        );
    }
}

export default App;
