package com.github.mmolimar.ksql.jdbc

import java.sql.{Connection, SQLException, SQLFeatureNotSupportedException}
import java.util.{Collections, Properties}

import com.github.mmolimar.ksql.jdbc.utils.TestUtils._
import io.confluent.ksql.rest.client.{KsqlRestClient, RestResponse}
import io.confluent.ksql.rest.entity._
import io.confluent.ksql.rest.server.computation.CommandId
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

class KsqlConnectionSpec extends WordSpec with Matchers with MockFactory {

  "A KsqlConnection" when {

    "validating specs" should {
      val values = KsqlConnectionValues("localhost", 8080, Map.empty[String, String])
      val ksqlRestClient = mock[MockableKsqlRestClient]
      val ksqlConnection = new KsqlConnection(values, new Properties) {
        override def init: KsqlRestClient = ksqlRestClient
      }

      "throw not supported exception if not supported" in {
        val methods = implementedMethods[KsqlConnection]
        reflectMethods[KsqlConnection](methods, false, ksqlConnection)
          .foreach(method => {
            assertThrows[SQLFeatureNotSupportedException] {
              method()
            }
          })
      }

      "work if implemented" in {

        assertThrows[SQLException] {
          ksqlConnection.isClosed
        }
        ksqlConnection.getTransactionIsolation should be(Connection.TRANSACTION_NONE)
        ksqlConnection.setClientInfo(new Properties)

        (ksqlRestClient.makeKsqlRequest _).expects(*)
          .returns(RestResponse.successful[KsqlEntityList](new KsqlEntityList))
        ksqlConnection.setClientInfo("", "")
        assertThrows[SQLException] {
          (ksqlRestClient.makeKsqlRequest _).expects(*)
            .returns(RestResponse.erroneous(new KsqlErrorMessage(-1, "", Collections.emptyList[String])))
          ksqlConnection.setClientInfo("", "")
        }

        ksqlConnection.isReadOnly should be(true)

        (ksqlRestClient.makeStatusRequest _: () => RestResponse[CommandStatuses]).expects
          .returns(RestResponse.successful[CommandStatuses]
            (new CommandStatuses(Collections.emptyMap[CommandId, CommandStatus.Status])))
        ksqlConnection.isValid(0) should be(true)

        Option(ksqlConnection.getMetaData) should not be (None)

        Option(ksqlConnection.createStatement) should not be (None)
        assertThrows[SQLFeatureNotSupportedException] {
          ksqlConnection.createStatement(-1, -1)
        }
        ksqlConnection.setAutoCommit(true)
        ksqlConnection.setAutoCommit(false)
        ksqlConnection.getAutoCommit should be(false)
        ksqlConnection.getWarnings should be(None.orNull)
        ksqlConnection.getCatalog should be(None.orNull)
        ksqlConnection.setCatalog("test")
        ksqlConnection.getCatalog should be(None.orNull)

        (ksqlRestClient.close _).expects
        ksqlConnection.close
        ksqlConnection.isClosed should be(true)
        ksqlConnection.commit
      }
    }
  }

  "A ConnectionNotSupported" when {

    "validating specs" should {

      "throw not supported exception if not supported" in {

        val resultSet = new ConnectionNotSupported
        reflectMethods[ConnectionNotSupported](Seq.empty, false, resultSet)
          .foreach(method => {
            assertThrows[SQLFeatureNotSupportedException] {
              method()
            }
          })
      }
    }
  }

}
