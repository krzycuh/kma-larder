package pl.kmazurek.larder.application.usecase.pantry

import pl.kmazurek.larder.application.usecase.common.ConflictException
import pl.kmazurek.larder.application.usecase.common.ForbiddenException
import pl.kmazurek.larder.application.usecase.common.NotFoundException

class PantryNotFoundException(message: String) : NotFoundException(message)

class PantryAccessDeniedException(message: String) : ForbiddenException(message)

class PantryMemberNotFoundException(message: String) : NotFoundException(message)

class AlreadyPantryMemberException(message: String) : ConflictException(message)

class CannotRemovePantryMemberException(message: String) : ForbiddenException(message)

class ProductNotFoundException(message: String) : NotFoundException(message)

class DuplicateProductException(message: String) : ConflictException(message)

class BatchNotFoundException(message: String) : NotFoundException(message)

class InsufficientStockException(message: String) : ConflictException(message)
