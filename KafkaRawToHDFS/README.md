# KafkaRawToHDFS

This processor is designed to continuously poll a Kafka topic, hold the data until it grows to a (configurable) size and write it's raw content to HDFS.

# Processors used:
GetKafka
MergeContent
UpdateAttribute
PutHDFS
