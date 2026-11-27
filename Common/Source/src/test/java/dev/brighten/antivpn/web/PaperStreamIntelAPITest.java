/*
 * Copyright 2026 LucasTHCR
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

package dev.brighten.antivpn.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.junit.jupiter.api.Test;

class PaperStreamIntelAPITest {

  @Test
  void torExitIsAProxy() throws Exception {
    VPNResponse response =
        PaperStreamIntelAPI.fromJson(
            "185.220.101.1",
            new JSONObject(
                "{\"asn\":60729,\"community\":false,\"country\":\"DE\",\"hosting\":true,"
                    + "\"ip\":\"185.220.101.1\",\"network\":\"hosting\",\"proxy\":false,"
                    + "\"reasons\":[\"tor-exit\"],\"score\":95,\"seen_by\":0,\"tor\":true,\"vpn\":true}"));

    assertTrue(response.isSuccess());
    assertTrue(response.isProxy());
    assertEquals("60729", response.getAsn());
    assertEquals("DE", response.getCountryCode());
    assertEquals("PaperStream Intel (tor)", response.getMethod());
  }

  @Test
  void homeConnectionIsClean() throws Exception {
    VPNResponse response =
        PaperStreamIntelAPI.fromJson(
            "2.212.37.97",
            new JSONObject(
                "{\"asn\":6805,\"community\":false,\"country\":\"DE\",\"hosting\":false,"
                    + "\"ip\":\"2.212.37.97\",\"network\":\"end_user\",\"proxy\":false,"
                    + "\"reasons\":[],\"score\":0,\"seen_by\":0,\"tor\":false,\"vpn\":false}"));

    assertTrue(response.isSuccess());
    assertFalse(response.isProxy());
  }

  @Test
  void hostingAloneIsNotAProxy() throws Exception {
    VPNResponse response =
        PaperStreamIntelAPI.fromJson(
            "1.1.1.1",
            new JSONObject(
                "{\"asn\":13335,\"country\":\"US\",\"hosting\":true,\"network\":\"hosting\","
                    + "\"proxy\":false,\"tor\":false,\"vpn\":false}"));

    assertFalse(response.isProxy());
  }

  @Test
  void errorAnswerFails() throws Exception {
    VPNResponse response =
        PaperStreamIntelAPI.fromJson("x", new JSONObject("{\"error\":\"not an IP address\"}"));

    assertFalse(response.isSuccess());
  }
}
