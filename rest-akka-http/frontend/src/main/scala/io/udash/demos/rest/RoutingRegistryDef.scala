package io.udash.demos.rest

import io.udash._
import io.udash.demos.rest.model.{ContactId, PhoneBookId}

import scala.util.Try

class RoutingRegistryDef extends RoutingRegistry[RoutingState] {
  def matchUrl(url: Url): RoutingState =
    url2State.applyOrElse("/" + url.value.stripPrefix("/").stripSuffix("/"), (x: String) => ErrorState)

  def matchState(state: RoutingState): Url =
    Url(state2Url.apply(state))

  private val url2State: PartialFunction[String, RoutingState] = {
    case "/" => IndexState
    case "/contact" => ContactFormState()
    case "/contact" / arg => ContactFormState(Try(ContactId(arg.toInt)).toOption)
    case "/book" => PhoneBookFormState()
    case "/book" / arg => PhoneBookFormState(Try(PhoneBookId(arg.toInt)).toOption)
  }

  private val state2Url: PartialFunction[RoutingState, String] = {
    case IndexState => "/"
    case ContactFormState(None) => "/contact"
    case ContactFormState(Some(ContactId(id))) => s"/contact/$id"
    case PhoneBookFormState(None) => "/book"
    case PhoneBookFormState(Some(PhoneBookId(id))) => s"/book/$id"
  }
}