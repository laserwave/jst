# JST (Joint Sentiment Topic Model) 

This is a java implementation of Joint Sentiment Topic Model. JST can be used for sentiment analysis and emotion detection.



# Usage

Method 1: Compile a jar using the jst.core.JST class as Main and execute.

Method 2: Run jst.core.Run.


# Extracted Topics

Following is the extracted topics using a chinese news dataset. Check the .stwords file in model directory to see all the topics. 

![topics](https://github.com/laserwave/jst/blob/master/data/topics.png)

# Lexicon

The format of sentiment or emotion lexicon file is as follows:

S senti_name_1 senti_name_2 ... senti_name_S

token_1 token_sentiment_distribution_1

token_2 token_sentiment_distribution_2

.

.

token_m sentiment_distribution_m

where S is the number of sentiment and token sentiment distribution is a S-dimensional vector separated by a blank.

Refer to lexicon.txt in the data directory.

# Data Format

N

doc_sentiment_distribution_1#word_1 word_2 ... word_d1

doc_sentiment_distribution_2#word_1 word_2 ... word_d2

.

.

doc_sentiment_distribution_N#word_1 word_2 ... word_dN

where N is the number of documents, document sentiment distribution is a S-dimensional vector separated by a blank.

# Demo

A demo chinese dataset has been provided in the data directory. Segmentation of Chinese text or tokenization of English text should be done for preprocessing. Run jst.core.Run to train a new model. 


Author
============

 * zhikai.zhang 
 * Email <zhangzhikai@seu.edu.cn>
 * Blog <http://zhikaizhang.cn>

