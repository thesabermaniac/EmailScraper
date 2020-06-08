# EmailScraper
A Java program that scrapes the internet for n email addresses and uploads them to a database.

1) Starts with url x
2) Collects all urls and email addresses in that web page and stores them in a set
3) Iterates through the set of urls, repeating step 2 until n email addresses are reached, uploading every n/10 emails to the database.
4) Empties the remaining emails from the set into the database.
