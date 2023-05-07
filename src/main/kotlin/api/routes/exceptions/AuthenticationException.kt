package api.routes.exceptions

class AuthenticationException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {}