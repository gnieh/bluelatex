/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.blue.couch

case class Paper(_id: String,
                 title: String,
                 authors: Set[String],
                 reviewers: Set[String],
                 cls: String,
                 references: List[String] = Nil,
                 enabled_modules: List[String] = Nil,
                 tags: List[String] = Nil,
                 branch: String = "master",
                 _rev: Option[String] = None)

object PaperViews {

  val authors = """function(doc) {
 if(doc.authors.length !== undefined) {
   emit(doc._id, doc.authors);
 }
}"""

  val reviewers = """function(doc) {
 if(doc.reviewers.length !== undefined) {
   emit(doc._id, doc.reviewers);
 }
}"""

  val my = """function(doc) {
 if(doc.authors.length !== undefined) {
   for(i in doc.authors) {
     emit(doc.authors[i], 'author');
   }
 }
 if(doc.reviewers.length !== undefined) {
   for(i in doc.reviewers) {
     emit(doc.reviewers[i], 'reviewer');
   }
 }
}"""

  val people = """function(doc) {
 var people = {};
 if(doc.authors.length !== undefined) {
   for(i in doc.authors) {
      people[doc.authors[i]] = 'author';
   }
 }
 if(doc.reviewers.length !== undefined) {
   for(i in doc.reviewers) {
      people[doc.reviewers[i]] = 'reviewer';
   }
 }
 emit(doc._id, people);
}"""

}
