## KafkaRawToHDFS

This processor is designed to continuously poll a Kafka topic, hold the data until it grows to a (configurable) size and write it's raw content to HDFS.

### Processors used:
[GetKafka](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.kafka.GetKafka/index.html)

[MergeContent](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.standard.MergeContent/index.html)

[UpdateAttribute](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.attributes.UpdateAttribute/index.html)

[PutHDFS](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.hadoop.PutHDFS/index.html)
