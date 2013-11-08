/*
 * Copyright  1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package blue

import java.io.{
  File,
  FileInputStream,
  FileOutputStream,
  IOException,
  PrintStream
}
import java.nio.channels.FileChannel
import java.nio.charset.{
  Charset,
  CharsetDecoder
}

object sed {

    def replaceAll(file: File, substitue: String, substituteReplacement: String): Unit = {

        val pattern = substitue.r

        // Open the file and then get a channel from the stream
        val fis = new FileInputStream(file)
        val fc = fis.getChannel

        // Get the file's size and then map it into memory
        val sz = fc.size
        val bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz)

        val charset = Charset.forName("UTF-8")
        val decoder = charset.newDecoder
        val cb = decoder.decode(bb)

        println(substituteReplacement)
        val outString = pattern.replaceAllIn(cb, substituteReplacement)

        val fos = new FileOutputStream(file.getAbsolutePath)
        val ps =new PrintStream(fos)
        try {
          ps.print(outString)
        } finally {
          ps.close()
          fos.close()
        }
    }
}

