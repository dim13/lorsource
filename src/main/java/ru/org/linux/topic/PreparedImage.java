/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.topic;

import ru.org.linux.gallery.Image;
import ru.org.linux.util.image.ImageInfo;

public class PreparedImage {
  private final String mediumName;
  private final String fullName;

  private final ImageInfo mediumInfo;
  private final ImageInfo fullInfo;

  private final Image image;
  private final boolean medium2x;

  public PreparedImage(String mediumName, ImageInfo mediumInfo, String fullName, ImageInfo fullInfo, Image image, boolean medium2x) {
    this.mediumName = mediumName;
    this.mediumInfo = mediumInfo;
    this.fullName = fullName;
    this.fullInfo = fullInfo;
    this.image = image;
    this.medium2x = medium2x;
  }

  public String getMediumName() {
    return mediumName;
  }

  public ImageInfo getMediumInfo() {
    return mediumInfo;
  }

  public String getFullName() {
    return fullName;
  }

  public ImageInfo getFullInfo() {
    return fullInfo;
  }

  public Image getImage() {
    return image;
  }

  public String srcset() {
    if (medium2x) {
      return "srcset=\""+image.getMedium2x()+" 2x\"";
    } else {
      return "";
    }
  }
}
