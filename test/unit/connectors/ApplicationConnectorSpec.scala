/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.ApplicationConnector
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ApplicationConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val connector = new ApplicationConnector {
      override val http = WSHttp
      override val applicationBaseUrl: String = wireMockUrl
    }
  }

  "fetchAllSubscribedApplications" should {
    "retrieve all applications" in new Setup {
      val uri = "/application/subscriptions"
      val body = "[\n  {\n    \"apiIdentifier\": {\n      \"context\": \"individual-benefits\",\n      \"version\": \"1.0\"\n    },\n    \"applications\": [\n      \"a97541e8-f93d-4d0a-ab0b-862e63204b7d\",\n      \"4bf49df9-523a-4aa3-a446-683ff24b619f\",\n      \"42695949-c7e8-4de9-a443-15c0da43143a\"\n    ]\n  }]"
      stubFor(get(urlEqualTo(uri)).willReturn(aResponse().withStatus(200).withBody(body)))
      val result: Seq[SubscriptionResponse] = await(connector.fetchAllSubscriptions())
      verify(1, getRequestedFor(urlPathEqualTo(uri)).withHeader("Authorization", equalTo(authToken)))
      result.head.apiIdentifier.context shouldBe "individual-benefits"
    }
  }

  "approveUplift" should {
    "send Authorisation and return OK if the uplift was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/approve-uplift")).willReturn(aResponse().withStatus(204)))
      val result = await(connector.approveUplift(applicationId, gatekeeperId))
      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/approve-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))

      result shouldBe ApproveUpliftSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/approve-uplift")).willReturn(aResponse().withStatus(412)
        .withBody( """{"code"="INVALID_STATE_TRANSITION","message":"Application is not in state 'PENDING_GATEKEEPER_APPROVAL'"}""")))

      intercept[PreconditionFailed](await(connector.approveUplift(applicationId, gatekeeperId)))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/approve-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))
    }
  }

  "rejectUplift" should {
    "send Authorisation and return Ok if the uplift rejection was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      val rejectionReason = "A similar name is already taken by another application"
      stubFor(post(urlEqualTo(s"/application/$applicationId/reject-uplift")).willReturn(aResponse().withStatus(204)))

      val result = await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/reject-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo(
          s"""{"gatekeeperUserId":"$gatekeeperId","reason":"$rejectionReason"}""")))
      }

    "hande 412 preconditions failed" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      val rejectionReason = "A similar name is already taken by another application"
      stubFor(post(urlEqualTo(s"/application/$applicationId/reject-uplift")).willReturn(aResponse().withStatus(412)
        .withBody( """{"code"="INVALID_STATE_TRANSITION","message":"Application is not in state 'PENDING_GATEKEEPER_APPROVAL'"}""")))

      intercept[PreconditionFailed](await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason)))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/reject-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo(
          s"""{"gatekeeperUserId":"$gatekeeperId","reason":"$rejectionReason"}""")))
    }
  }

  "resend verification email" should {
    "send Verification request and return OK if the resend was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/resend-verification")).willReturn(aResponse().withStatus(204)))
      val result = await(connector.resendVerification(applicationId, gatekeeperId))
      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/resend-verification"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))

      result shouldBe ResendVerificationSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/resend-verification")).willReturn(aResponse().withStatus(412)
        .withBody( """{"code"="INVALID_STATE_TRANSITION","message":"Application is not in state 'PENDING_REQUESTOR_VERIFICATION'"}""")))

      intercept[PreconditionFailed](await(connector.resendVerification(applicationId, gatekeeperId)))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/resend-verification"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))
    }
  }


  "fetchApplicationsWithUpliftRequest" should {
    "retrieve all applications pending uplift approval" in new Setup {
      stubFor(get(urlEqualTo(s"/gatekeeper/applications")).willReturn(aResponse().withStatus(200)
        .withBody("[]")))

      val result = await(connector.fetchApplicationsWithUpliftRequest())

      verify(1, getRequestedFor(urlPathEqualTo("/gatekeeper/applications"))
        .withHeader("Authorization", equalTo(authToken)))
    }

    "propagate FetchApplicationsFailed exception" in new Setup {
      stubFor(get(urlEqualTo(s"/gatekeeper/applications")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApplicationsFailed](await(connector.fetchApplicationsWithUpliftRequest()))

      verify(1, getRequestedFor(urlPathEqualTo(s"/gatekeeper/applications"))
        .withHeader("Authorization", equalTo(authToken)))
    }
  }

  "fetchAllApplicationsBySubscription" should {
    "retrieve all applications subscribed to a specific API" in new Setup {
      stubFor(get(urlEqualTo(s"/application?subscribesTo=some-context")).willReturn(aResponse().withStatus(200)
        .withBody("[]")))

      val result = await(connector.fetchAllApplicationsBySubscription("some-context"))

      verify(1, getRequestedFor(urlPathEqualTo("/application?subscribesTo=some-context"))
        .withHeader("Authorization", equalTo(authToken)))
    }

    "propagate fetchAllApplicationsBySubscription exception" in new Setup {
      stubFor(get(urlEqualTo(s"/application?subscribesTo=some-context")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApplicationsFailed](await(connector.fetchAllApplicationsBySubscription("some-context")))

      verify(1, getRequestedFor(urlPathEqualTo(s"/application?subscribesTo="))
        .withHeader("Authorization", equalTo(authToken)))
    }
  }

  "fetchAllApplications" should {
    "retrieve all applications" in new Setup {
      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withStatus(200)
        .withBody("[]")))

      val result = await(connector.fetchAllApplications())

      verify(1, getRequestedFor(urlPathEqualTo("/application"))
        .withHeader("Authorization", equalTo(authToken)))
    }

    "propagate fetchAllApplications exception" in new Setup {
      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApplicationsFailed](await(connector.fetchAllApplications()))

      verify(1, getRequestedFor(urlPathEqualTo(s"/application"))
        .withHeader("Authorization", equalTo(authToken)))
    }
  }
}
