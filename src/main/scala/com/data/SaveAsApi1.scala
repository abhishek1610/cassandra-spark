package com.data

import com.datastax.spark.connector.cql._
import com.datastax.spark.connector.types.{BigIntType, IntType, TextType}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._



  object SaveAsApi1 {
    def main(args: Array[String])
    {

    val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", "quickstart.cloudera")
    .setMaster("local[*]")
    .setAppName("app2")

  val conn = CassandraConnector(conf)
  conn.withSessionDo { session =>
    session.execute("DROP KEYSPACE IF EXISTS write_test")
    session.execute("CREATE KEYSPACE IF NOT EXISTS write_test WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }")
  }

  import com.datastax.spark.connector._
  val sc = new SparkContext(conf)

  val pkey = ColumnDef("key", PartitionKeyColumn, IntType)
  val group = ColumnDef("group", ClusteringColumn(0), BigIntType)
  val value = ColumnDef("value", RegularColumn, TextType)
  val table = TableDef("write_test", "new_kv_table", Seq(pkey), Seq(group), Seq(value))
  val rows = Seq((1, 1L, "value1"), (2, 2L, "value2"), (3, 3L, "value3"))
  sc.parallelize(rows).saveAsCassandraTableEx(table, SomeColumns("key", "group", "value"))

  // verify
  conn.withSessionDo { session =>
    val result = session.execute("SELECT * FROM write_test.new_kv_table").all().asScala
    require(result.size == 3)
    result foreach println
  }

  sc.stop()
}
}