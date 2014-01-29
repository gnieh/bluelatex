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
package gnieh.blue
package compile
package impl

import akka.actor.{
  Actor,
  ActorRef
}
import akka.util.Timeout

import scala.concurrent.duration._

import scala.util.Try

import org.osgi.framework.BundleContext

import common._

/** This actor handles compilation of a paper.
 *  An instance of this actor is not eternally resident, if no
 *  compilation message was received within a session timeout,
 *  this actor is destroyed.
 *
 *  @author Lucas Satabin
 *
 */
class CompilationActor(
  bndContext: BundleContext,
  synchro: SynchroServer,
  configuration: PaperConfiguration,
  paperId: String,
  defaultSettings: CompilerSettings,
  val logger: Logger
) extends Actor with Logging {

  import OsgiUtils._

  @inline
  implicit def ec = context.system.dispatcher

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(Duration.Zero, self, Compile)
  }

  def receive = receiving(Set(), defaultSettings)

  def receiving(clients: Set[ActorRef], settings: CompilerSettings): Receive = {
    case Compile =>

      implicit val timeout = Timeout(settings.timeout.seconds)


      // dump the files before printing
      synchro.persist(paperId)

      for(compiler <- bndContext.get[Compiler]) {

        // TODO iddeally the number of times we run the latex compiler, and bibtex,
        // and the index compiler should be smarter than this.
        // For the moment, we run only once, but we could make it configurable if the compilation
        // occurs on its own compilation server
        // it is also not necessary to run bibtex if no bibliography is to be generated
        // the settings should be able to handle this properly
        val res = for {
          // if the compiler is defined, we first compile the paper
          res <- compiler.compile(paperId, settings)
          // we run bibtex on it if the compilation succeeded
          _ <- compiler.bibtex(paperId, settings)
        } yield res

        // and we send back the answer to the clients
        for(client <- clients)
          client ! res

        // and listen again with an empty list of clients
        context.become(receiving(Set(), settings))

      }

      // schedule the next compilation after the configured interval
      context.system.scheduler.scheduleOnce(settings.interval.seconds, self, Compile)

    case settings @ CompilerSettings(_, _, _, _) =>
      // settings were changed, take them immediately into account
      context.become(receiving(clients, settings))

    case Register(client) =>

      context.become(receiving(clients + client, settings))

  }

}

case object Compile
case class Register(client: ActorRef)

