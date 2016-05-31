package controllers

import javax.inject.Inject

import models.Message
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext

class MessagesController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit context : ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents
{
  def collection = database.map( _.collection[JSONCollection]("messages"))

  def getMessages(receiver: String) = Action.async
  {
    collection.flatMap
    {
      _.find(Json.obj("receiver" -> receiver))
        .cursor[Message](ReadPreference.primary)
          .collect[List]()
    }.map
    {
      data => Ok(Json.toJson(data))
    }
  }

  def sendMessage = Action.async
  {
    request =>
    {
      val message = request.body.asJson.flatMap
      {
        body => Json.fromJson[Message](body).asOpt
      }

      collection
        .flatMap
        {
          _.insert[Message](message.get)
        }
        .map
        {
          result =>
          {
            def createResponse(message: String) =
            {
              Json.obj("result" -> message)
            }

            val error = result.errmsg
            if(error.isEmpty) Ok(createResponse("success")) else BadRequest(createResponse(error.get))

          }
        }
    }
  }
}
