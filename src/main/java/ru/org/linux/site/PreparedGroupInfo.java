/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site;

import ru.org.linux.util.bbcode.ParserUtil;

import java.sql.Connection;

public class PreparedGroupInfo {
  private final Group group;
  private final String longInfo;

  @Deprecated
  public PreparedGroupInfo(Connection db, Group group) {
    this.group = group;

    if (group.getLongInfo()!=null) {
      longInfo = ParserUtil.bb2xhtml(group.getLongInfo()); // TODO нет доступа к БД ?
    } else {
      longInfo = null;
    }
  }

  public PreparedGroupInfo(Group group, String longInfo) {
    this.group = group;
    this.longInfo = longInfo;
  }

  public Group getGroup() {
    return group;
  }

  public String getLongInfo() {
    return longInfo;
  }

  public String getInfo() {
    return group.getInfo();
  }

  public int getId() {
    return group.getId();
  }
}
