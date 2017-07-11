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
my_auth = requests_oauthlib.OAuth1(CONSUMER_KEY, CONSUMER_SECRET,ACCESS_TOKEN, ACCESS_SECRET)

def get_tweets():
    # Query params according for Twitter's streaming API
    # Reference: https://dev.twitter.com/streaming/reference/post/statuses/filter
    sports_leagues = nba,mlb,nfl,nhl,mls
    url = 'https://stream.twitter.com/1.1/statuses/filter.json'

    query_data = [('language', 'en'),('track', sports_leagues)]
    query_url = url + '?' + '&'.join([str(t[0]) + '=' + str(t[1]) for t in query_data])

    response = requests.get(query_url, auth=my_auth, stream=True)
    print(query_url, response)
    return response

def send_tweets_to_spark(response, tcp_connection):
    for line in response.iter_lines():
        try:
            full_tweet = json.loads(line)
            tweet_text = full_tweet['text']
            print("Tweet text:" + tweet_text)
            print("--------------------------------------")
            tcp_connection.send(tweet_text + '\n')
        except:
            e = sys.exc_info()[0]
            print("Error: %s" % e)

TCP_IP = "localhost"
TCP_PORT = 9009
conn = None
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((TCP_IP, TCP_PORT))
s.listen(1)
print("Waiting for TCP connection...")
conn, addr = s.accept()
print("Connected... Starting getting tweets.")
resp = get_tweets()
send_tweets_to_spark(resp, conn)
