/*
 * Copyright (c) 2018 Exam Consortium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed
 * on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import play.libs.Json;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static org.fest.assertions.Assertions.assertThat;

public class JsonFilterTest {

    @Test
    public void testFilterProperties() throws Exception {
        final String json = FileUtils
                .readFileToString(new File(Objects.requireNonNull(getClass().getClassLoader()
                                .getResource("jsonfilter_testdata.json")).toURI()),
                        "UTF-8");
        final JsonNode root = Json.parse(json);
        final JsonNode item = root.get("item");
        final JsonNode description = item.get("description");
        final ArrayNode offers = (ArrayNode) item.get("offers");

        final String[] filters = new String[]{"creator", "modifier"};
        assertThatJsonHasProperties(root, filters);
        assertThatJsonHasProperties(item, filters);
        assertThatJsonHasProperties(description, filters);
        offers.forEach(offer -> assertThatJsonHasProperties(offer, filters));

        // Filter json
        JsonFilter.filterProperties(root, filters);
        assertThatJsonDoesNotHaveProperties(root, filters);
        assertThatJsonDoesNotHaveProperties(item, filters);
        assertThatJsonDoesNotHaveProperties(description, filters);
        offers.forEach(offer -> assertThatJsonDoesNotHaveProperties(offer, filters));
    }

    private void assertThatJsonHasProperties(JsonNode jsonNode, String... props) {
        Arrays.asList(props).forEach(p -> assertThat(jsonNode.has(p)).isTrue());
    }

    private void assertThatJsonDoesNotHaveProperties(JsonNode jsonNode, String... props) {
        Arrays.asList(props).forEach(p -> assertThat(jsonNode.has(p)).isFalse());
    }
}