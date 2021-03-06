/* Copyright (c) 2017 Ryuichi Saito, Keio University. All right reserved. */
package jp.ac.keio.sdm.ConcurrentLogControl

import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Created by Ryuichi on 3/23/2017 AD.
  */
object Streaming extends LogControlExperimentFigure{

  /*val japaneseUnicodeBlock = new mutable.HashSet[UnicodeBlock]()
      .+=(UnicodeBlock.HIRAGANA, UnicodeBlock.HIRAGANA, UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)*/

  def createTweetsWordCount(ssc: StreamingContext, windowLength: Long, slidingInterval: Long): StreamingContext = {

    // Create a Twitter DStream for the input source.
    val twitterStream = TwitterUtils.createStream(ssc, None)

    // Parse the tweets and gather the hashTags.
    // val hashTagStream = twitterStream.filter(_.getLang == "en").map(_.getText).flatMap(_.split(" ")).filter(_.startsWith("#"))
    // Throw Exception
    val hashTagStream = twitterStream.map(_.getText).flatMap(_.split(" "))
      .map(s => {
        val validator = new Validator

        // Tentative Check Exception 1
        try {
          if(validator.isExistsHashTag(s)) {
            throw new HashTagException()
          }
        } catch {
          case e: Exception =>
            logger.error(MessageController.getMessage("EXP-E000001"), e)
        }

        // Tentative Check Exception 2
        try {
          if(validator.isJpananesUnicodeBlock(s)) {
            throw new UnicodeBlockException()
          }
        } catch {
          case e: Exception =>
            logger.error(MessageController.getMessage("EXP-E000002"), e)
        }

        // Tentative Check Exception 3
        try {
          if (!validator.isChackWordLengh(s)) {
            throw new WordLengthException()
          }
        } catch {
          case e: Exception =>
            logger.error(MessageController.getMessage("EXP-E000003"), e)
        }

        // Tentative Check Exception 4
        try {
          if (!validator.isExistsNumeric(s)) {
            throw new NumericException()
          }
        } catch {
          case e: Exception =>
            logger.error(MessageController.getMessage("EXP-E000004"), e)
        }

        // Tentative Runtime Exception 1
        val forwardMessage = s(10)

        val Message = s.toInt

      })

    // Use Filtering Method.
    /*val hashTagStream = twitterStream.filter(_.getLang == "en").map(_.getText).flatMap(_.split(" "))
        .map(s => try {s(10000)} catch { case runtime : RuntimeException => { LogCache.putIfAbsent("EXP-E000001", runtime)}})*/
    // Output to file.
    /*val hashTagStream = twitterStream.filter(_.getLang == "en").map(_.getText).flatMap(_.split(" "))
      .map(s => try {s(10000)} catch { case runtime : RuntimeException => { logger.warn("EXP-E000001", runtime)}})*/

    // Compute the counts of each hashtag by window.
    // Reduce last 16 seconds of data, every 8 seconds
    val windowedhashTagCountStream = hashTagStream.map((_, 1)).reduceByKeyAndWindow((x: Int, y: Int) => x + y, Seconds(windowLength), Seconds(slidingInterval))
    // Development Mode
      windowedhashTagCountStream.saveAsTextFiles("tweets")
    // Product Mode
    // windowedhashTagCountStream.saveAsTextFiles("s3://aws-logs-757020086170-us-west-2/elasticmapreduce/tweets/tweets")
    ssc
  }
}