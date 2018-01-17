/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package schrodinger;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.NewUserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserTest extends TestBase {

    @Test
    public void createUser() {

        //@formatter:off
        NewUserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                    .addAttributeValue("name", "jdoe222323")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "john")
                    .addAttributeValue(UserType.F_FAMILY_NAME, "doe")
                    .and()
                .and()
            .clickSave();

//        user.selectTabProjections().and()
//            .selectTabPersonas().and()
//            .selectTabAssignments().and()
//            .selectTabTasks().and()
//            .selectTabDelegations().and()
//            .selectTabDelegatedToMe().and()
        //@formatter:on

        screenshot("create");

        ListUsersPage users = user.listUsers();

        // todo validation
    }
}
