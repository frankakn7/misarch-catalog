package org.misarch.catalog.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import org.misarch.catalog.graphql.dataloader.CategoryCharacteristicDataLoader
import org.misarch.catalog.graphql.dataloader.CategoryDataLoader
import org.misarch.catalog.graphql.dataloader.ProductDataLoader
import org.misarch.catalog.graphql.model.Category
import org.misarch.catalog.graphql.model.CategoryCharacteristic
import org.misarch.catalog.graphql.model.Product
import org.misarch.catalog.graphql.model.connection.*
import org.misarch.catalog.persistence.repository.CategoryRepository
import org.misarch.catalog.persistence.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Defines GraphQL queries
 *
 * @property productRepository repository for products
 * @property categoryRepository repository for categories
 */
@Component
class Query(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : Query {

    private val logger = LoggerFactory.getLogger(Query::class.java)

    @GraphQLDescription("Get all products")
    suspend fun products(
        @GraphQLDescription("Number of items to return")
        first: Int? = null,
        @GraphQLDescription("Number of items to skip")
        skip: Int? = null,
        @GraphQLDescription("Ordering")
        orderBy: ProductOrder? = null,
        @GraphQLDescription("Filtering")
        filter: ProductFilter? = null,
        dfe: DataFetchingEnvironment
    ): ProductConnection {
        val start = System.currentTimeMillis()
        logger.info("products query start: first={}, skip={}, isPubliclyVisible={}", first, skip, filter?.isPubliclyVisible)
        val connection = ProductConnection(first, skip, filter, null, orderBy, productRepository, dfe.authorizedUserOrNull)
        logger.info("products query connection built in {}ms", System.currentTimeMillis() - start)
        return connection
    }

    @GraphQLDescription("Get all categories")
    suspend fun categories(
        @GraphQLDescription("Number of items to return")
        first: Int? = null,
        @GraphQLDescription("Number of items to skip")
        skip: Int? = null,
        @GraphQLDescription("Ordering")
        orderBy: CategoryOrder? = null,
        dfe: DataFetchingEnvironment
    ): CategoryConnection {
        return CategoryConnection(first, skip, null, orderBy, categoryRepository, dfe.authorizedUserOrNull)
    }

    @GraphQLDescription("Get a product by id")
    fun product(
        @GraphQLDescription("The id of the product")
        id: UUID,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<Product> {
        return dfe.getDataLoader<UUID, Product>(ProductDataLoader::class.simpleName!!).load(id).thenApply {
            if (!it.isPubliclyVisible) {
                dfe.authorizedUser.checkIsEmployee()
            }
            it
        }
    }

    @GraphQLDescription("Get a category by id")
    fun category(
        @GraphQLDescription("The id of the category")
        id: UUID,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<Category> {
        return dfe.getDataLoader<UUID, Category>(CategoryDataLoader::class.simpleName!!).load(id)
    }

    @GraphQLDescription("Get a characteristic by id")
    fun categoryCharacteristic(
        @GraphQLDescription("The id of the characteristic")
        id: UUID,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<CategoryCharacteristic> {
        return dfe.getDataLoader<UUID, CategoryCharacteristic>(CategoryCharacteristicDataLoader::class.simpleName!!).load(id)
    }

}