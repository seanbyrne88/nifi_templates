## KafkaRawToHDFS

This template is designed to continuously poll a Kafka topic, hold the data until it grows to a (configurable) size and write it's raw content to HDFS.

### Processors used:
[GetKafka](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.kafka.GetKafka/index.html): Pulls data from a Kafka source. Configure your topic name and consumer group id within this processor.

[ReplaceText](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.standard.ReplaceText/index.html): Prepends kafka message content with a timestamp and pipe "|" delimiter to be loaded to HDFS. Uses Replacement Strategy "Prepend".

[MergeContent](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.standard.MergeContent/index.html): Holds data until the flow file reaches a suitable (configurable) size to be loaded to HDFS. Configure "Minimum group size" setting in the processor to decide the size of the files you want to put on HDFS. You also may need to configure the demarcator for separate kafka messages. By default this will be a new line which is in plaintext in the demarcator setting.

[UpdateAttribute](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.attributes.UpdateAttribute/index.html): Updates the FlowFile filename attribute to be a timestamp.

[PutHDFS](https://nifi.apache.org/docs/nifi-docs/components/org.apache.nifi.processors.hadoop.PutHDFS/index.html): Loads data to HDFS.
