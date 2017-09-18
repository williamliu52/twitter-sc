import socket
import sys
import requests
import requests_oauthlib
import json

# Twitter access tokens
ACCESS_TOKEN = '393038677-QGiOscLRjyS46BWwBrjG4DYukdNS7m16h3PGSAoz'
ACCESS_SECRET = 'W8BmcuB8Ezf0IScLJroGZscp97z4NCH4lllHlHR9VBXCt'
CONSUMER_KEY = '1piLrcfKuRxSi8rWpJ6j9C2pJ'
CONSUMER_SECRET = 'iMkNO6otUOVySGqOBYkHsXDDz5ewQc3x7nfEupdKZOHDKnbApL'
my_auth = requests_oauthlib.OAuth1(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_SECRET)

def get_tweets():
    # Query params according for Twitter's streaming API
    # Reference: https://dev.twitter.com/streaming/reference/post/statuses/filter
    sports_leagues = 'nba,mlb,nfl,nhl,mls,sctop10'
    url = 'https://stream.twitter.com/1.1/statuses/filter.json'

    # Build url to send to API
    query_data = [('language', 'en'),('track', sports_leagues)]
    query_url = url + '?' + '&'.join([str(t[0]) + '=' + str(t[1]) for t in query_data])

    response = requests.get(query_url, auth=my_auth, stream=True)
    return response

# Helper functions that extract tweet information such as video and photo url
def get_photo_url(tweet):
    try:
        photo_url = tweet['entities']['media'][0]['url']
    except:
        photo_url = "None"
    return photo_url

def get_video_url(tweet):
    try:
        video_url = tweet['extended_entities']['media'][0]['url']
    except:
        video_url = "None"
    return video_url

# Sends information about tweet to the Spark application
def send_tweets_to_spark(response, tcp_connection):
    for line in response.iter_lines():
        try:
            full_tweet = json.loads(line)
            retweeted = False
            if full_tweet.get('retweeted_status'):
                retweeted = True
                retweet = full_tweet['retweeted_status']
                tweet_text = retweet['text']
                tweet_favs = retweet['favorite_count']
                tweet_rts = retweet['retweet_count']
                video_url = get_video_url(retweet)
                photo_url = get_photo_url(retweet)
            else:
                tweet_text = full_tweet['text']
                tweet_favs = full_tweet['favorite_count']
                tweet_rts = full_tweet['retweet_count']
                video_url = get_video_url(full_tweet)
                photo_url = get_photo_url(full_tweet)

            print("Retweeted: " + str(retweeted))
            print("Tweet text: " + tweet_text)
            print("Favorites: " + str(tweet_favs))
            print("Retweets: " + str(tweet_rts))
            print("Photo URL: " + photo_url)
            print("Video URL: " + video_url)
            print("--------------------------------------")
            tcp_connection.send(tweet_text + ' ' + video_url + '\n')
        except:
            e = sys.exc_info()[0]
            print("Error: %s" % e)
            print("--------------------------------------")

# Connection constants for the socket
TCP_IP = "localhost"
TCP_PORT = 9009
conn = None

serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
clientSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# Create server socket to serve Tweets for 1 connection
serverSocket.bind((TCP_IP, TCP_PORT))
serverSocket.listen(1)

# Create mock client socket to connect and stream Tweets for testing
# clientSocket.connect((TCP_IP, TCP_PORT))

print("Waiting for TCP connection...")
conn, addr = serverSocket.accept()
print("Connected... Starting getting tweets.")
resp = get_tweets()
send_tweets_to_spark(resp, conn)
