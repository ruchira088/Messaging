package controllers

import javax.inject.Inject

import models.Message
import controllers.MessagesController.toJson
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class MessagesController @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit context : ExecutionContext) extends Controller
  with MongoController with ReactiveMongoComponents
{
  def collection = database.map( _.collection[JSONCollection]("messages"))

  def getMessages = Action.async
  {
    request =>
      {
        find(request.queryString)
          .map(data => Ok(Json.toJson[List[Message]](data)))
      }
  }

  def find(json: JsObject) : Future[List[Message]] = collection.flatMap
  {
    _.find(json)
      .cursor[Message](ReadPreference.primary)
      .collect[List]()
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

object MessagesController
{
  implicit def toJson(parameters: Map[String, Seq[String]]): JsObject =
  {
    Json.toJson(parameters.map
          {
            value =>
            {
              val (key, values) = value

              (key, if(values.length == 1) Json.toJson(values(0)) else Json.toJson(values))
            }
          })
      .as[JsObject]
  }
}