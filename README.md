# Twitter SportsCenter

SportsCenter powered by Twitter. See the sports highlights and stories that people are excited about in real time.

**NOTE:** This README and project is currently a WIP.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You will need the following programs installed on your computer: Python 2.7+, pip, Scala 2.11+, Java 8, Apache Spark 2.2+, SBT, NodeJS, and npm. Below are download links.

[Apache Spark](https://spark.apache.org/downloads.html)

[Scala (includes links for Java)](http://www.scala-lang.org/download/)

[SBT](http://www.scala-sbt.org/download.html)

[Python](https://www.python.org/downloads/)

[pip](https://pip.pypa.io/en/stable/installing/)

[NodeJS & npm](https://nodejs.org/en/)

### Installing

After installing all the programs listed in Prerequisites, clone the repo.
```bash
git clone https://github.com/williamliu52/twitter-sc.git
```

Next install the Python packages required, using pip.
```bash
pip install -r requirements.txt
```

Enter the `scala` directory and then compile with SBT; SBT may download additional packages the first time run which may take awhile.
```bash
cd scala
sbt compile
```

The final step is to setup the NodeJS + React application. Enter the `js` directory and install the required modules.
```bash
cd js
npm install
```

### Running
To run this application, you will need at least 3 terminal windows open. Starting from the project directory:

In terminal 1 run the NodeJS + React application. The `start` script runs the Node server at port 5000 and the React app at port 3000.
```bash
cd js
npm run start
```

In terminal 2 run the Python application to get tweets from Twitter.
```bash
. venv/bin/activate
# you should now see (venv) at each command line
python twitter_app.py
```

In terminal 3 start up the Scala application, which connects to the socket created by the Python app and sends data to NodeJS.
```bash
cd scala
sbt run
```

Open up `localhost:3000` in your browser. If everything goes right you should see a `Refresh Videos` button. Click the button and the page should begin to populate with embedded video tweets.

## Built With

* Python
* Apache Spark
* Scala
* NodeJS
* React


## Acknowledgments

* Spark streaming tutorial: https://www.toptal.com/apache/apache-spark-streaming-twitter
* Making Scala projects: http://www.learn4master.com/learn-how-to/how-to-package-a-scala-project-to-a-jar-file-with-sbt
* stackoverflow, Scala docs, Programming in Scala
* Twitter's various APIs:
    * Embed: https://dev.twitter.com/web/embedded-tweets
    * Streaming: https://dev.twitter.com/streaming/overview/request-parameters
* FullStack React: https://www.fullstackreact.com/articles/using-create-react-app-with-a-server/
