import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.get
import org.w3c.dom.set
import react.RBuilder
import react.dom.*
import react.router.dom.*

val body by lazy(LazyThreadSafetyMode.NONE) { document.body ?: throw Exception("No DOM") }
val previous by lazy(LazyThreadSafetyMode.NONE) {
  (0..body.childNodes.length).mapNotNull { body.childNodes[it] }
}

val root = document.createElement("div")

var uri: URI? = null

fun RBuilder.external(message: String, to: String) = window.location.run {
  console.log(message)
  console.log("Bye!")

  replace(to)
  a(to) { +message }
}

fun RBuilder.internal(message: String, to: String) = to.let { if (it.isBlank()) "/" else it }.let {
  console.log(message)
  console.log("Routing to:", it)

  routeLink(it) { +message }
  redirect(to = it)
}

@ExperimentalJsExport
@JsExport
fun launch(ready: (() -> Unit) -> Unit) = uri ?: URI(window.location.href).also {
  uri = it

  if (window.location.href != it.canonical) {
    window.location.href = it.canonical
  }

  ready(::startRender)
}

fun startRender() {
  body.style.display = ""

  render(root) {
    browserRouter {
      switch {
        route("/", true) {
          useReact(true)

          div("shell") {
            a("https://github.com/moltendorf/maez.red-frontend") {
              setProp("id", "forkme_banner")
              +"View on GitHub"
            }

            header {
              span("ribbon-outer") {
                span("ribbon-inner") {
                  h1 { routeLink("/") { +site.name } }
                  h2 { +site.description }
                }

                span("left-tail") { }
                span("right-tail") { }
              }
            }
            div {
              setProp("id", "no-downloads")
              span("inner") { }
            }
            span("banner-fix") { }
            section {
              setProp("id", "main_content")

              div { routeLink("/login") { +"Login" } }
              div { routeLink("/signup") { +"Sign Up" } }
              div { routeLink("/logout") { +"Logout" } }
              div { routeLink("/404") { +"Bad Link" } }
            }
          }
        }
        route("/(index|home|readme)") {
          useReact(true)
          redirect(to = "/")
        }
        route<dynamic>("/login(/signup)?(/success)?/:path*", true) { props ->
          useReact(true)

          val signup = (props.match.params[0] as? String).run { !isNullOrEmpty() }
          val success = (props.match.params[1] as? String).run { !isNullOrEmpty() }
          val path = (props.match.params.path as? String)?.takeUnless { it.isBlank() }?.let { "/$it" } ?: ""

          if (success) {
            URI(window.location.href).searchParams.get("code")
              .takeIf { l["auth0_code"]?.equals(it) ?: !it.isNullOrBlank() }?.let { code ->
                l["auth0_code"] = code

                if (signup) {
                  internal("Thanks for signing up on maez.red!!", path)
                } else {
                  internal("You're now logged in!!", path)
                }
              } ?: p { +"Something went wrong..." }
          } else {
            l["auth0_code"]?.let { code ->
              internal("You're logged in already it seems!", "/login/success$path?code=$code")
            } ?: external(
              "Sending you to auth0!",
              "https://maezred.us.auth0.com/authorize?client_id=RHUx3auDIzQcRWoXfDAa3kqJWQWUPf91&response_type=code&redirect_uri=${uri?.protocol}//${uri?.host}/login/success$path"
            )
          }
        }
        route<dynamic>("/signup/:path*", true) { props ->
          useReact(true)

          val path = (props.match.params.path as? String)?.takeUnless { it.isBlank() }?.let { "/$it" } ?: ""

          l["auth0_code"]?.let { code ->
            internal("You're logged in already it seems!", "/login/success$path?code=$code")
          } ?: external(
            "Sending you to auth0!",
            "https://maezred.us.auth0.com/authorize?client_id=RHUx3auDIzQcRWoXfDAa3kqJWQWUPf91&screen_hint=signup&response_type=code&redirect_uri=${uri?.protocol}//${uri?.host}/login/signup/success$path"
          )
        }
        route<dynamic>("/logout(/success)?/:path*", true) { props ->
          useReact(true)

          val success = (props.match.params[0] as? String).run { !isNullOrEmpty() }
          val path = (props.match.params.path as? String)?.takeUnless { it.isBlank() }?.let { "/$it" } ?: ""

          if (success) {
            l.removeItem("auth0_code")

            internal("See you next time! ... :(", path)
          } else {
            l["auth0_code"]?.let {
              external(
                "Sending you to auth0!",
                "https://maezred.us.auth0.com/v2/logout?client_id=RHUx3auDIzQcRWoXfDAa3kqJWQWUPf91&respondTo=${uri?.protocol}//${uri?.host}/logout/success$path"
              )
            } ?: internal(
              "Seems I already lost track ... remind me of who ... ?", "/logout/success$path"
            )
          }
        }
        route("*") {
          if (window.location.pathname != uri?.pathname) {
            window.location.pathname = window.location.pathname
          } else {
            useReact(false)
          }

          div {}
        }
      }
    }
  }
}

var rendering = false

fun useReact(enabled: Boolean) {
  if (rendering == enabled) {
    return
  }

  if (rendering) {
    body.removeChild(root)
    previous.forEach { body.appendChild(it) }
  } else {
    previous.forEach { body.removeChild(it) }
    body.appendChild(root)
  }

  rendering = !rendering
}
