package models

import play.api.libs.json.Json

case class Message(sender: String, receiver: String, contents: String)

object Message
{
  implicit val formatter = Json.format[Message]
}