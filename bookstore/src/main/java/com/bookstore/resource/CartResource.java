/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bookstore.resource;

/**
 *
 * @author ASUS
 */
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.CartNotFoundException;
import com.bookstore.exception.CustomerNotFoundException;
import com.bookstore.exception.InvalidInputException;
import com.bookstore.exception.OutOfStockException;
import com.bookstore.models.Book;
import com.bookstore.models.Cart;
import com.bookstore.models.CartItem;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ConcurrentHashMap;

@Path("/customers/{customerId}/cart")
public class CartResource {

    private static final ConcurrentHashMap<String, Cart> cartStore = new ConcurrentHashMap<>();
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Cart getCart(@PathParam("customerId") String customerId) {
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Get or create cart
        Cart cart = cartStore.get(customerId);
        if (cart == null) {
            cart = new Cart(customerId);
            cartStore.put(customerId, cart);
        }
        
        return cart;
    }
    
    @POST
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addItemToCart(
            @PathParam("customerId") String customerId,
            CartItem cartItem) {
        
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Validate cart item
        validateCartItem(cartItem);
        
        // Check if book exists and has enough stock
        BookResource bookResource = new BookResource();
        Book book;
        try {
            book = bookResource.getBookById(cartItem.getBookId());
        } catch (BookNotFoundException e) {
            throw new BookNotFoundException("Book with ID " + cartItem.getBookId() + " does not exist.");
        }
        
        // Check stock
        if (book.getStock() < cartItem.getQuantity()) {
            throw new OutOfStockException("Not enough stock for book with ID " + cartItem.getBookId() + 
                    ". Available: " + book.getStock() + ", Requested: " + cartItem.getQuantity());
        }
        
        // Get or create cart
        Cart cart = cartStore.get(customerId);
        if (cart == null) {
            cart = new Cart(customerId);
            cartStore.put(customerId, cart);
        }
        
        // Set the price from the book
        cartItem.setPrice(book.getPrice());
        
        // Add item to cart
        cart.addItem(cartItem);
        
        return Response.status(Response.Status.CREATED)
                .entity(cart)
                .build();
    }
    
    @PUT
    @Path("/items/{bookId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Cart updateCartItem(
            @PathParam("customerId") String customerId,
            @PathParam("bookId") String bookId,
            CartItem cartItem) {
        
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Get cart
        Cart cart = cartStore.get(customerId);
        if (cart == null) {
            throw new CartNotFoundException("Cart for customer with ID " + customerId + " does not exist.");
        }
        
        // Validate quantity
        if (cartItem.getQuantity() <= 0) {
            throw new InvalidInputException("Quantity must be greater than 0.");
        }
        
        // Check if book exists and has enough stock
        BookResource bookResource = new BookResource();
        Book book;
        try {
            book = bookResource.getBookById(bookId);
        } catch (BookNotFoundException e) {
            throw new BookNotFoundException("Book with ID " + bookId + " does not exist.");
        }
        
        // Check stock
        if (book.getStock() < cartItem.getQuantity()) {
            throw new OutOfStockException("Not enough stock for book with ID " + bookId + 
                    ". Available: " + book.getStock() + ", Requested: " + cartItem.getQuantity());
        }
        
        // Make sure the bookId in path matches the bookId in the item
        cartItem.setBookId(bookId);
        cartItem.setPrice(book.getPrice());
        
        // Update cart
        cart.updateItemQuantity(bookId, cartItem.getQuantity());
        
        return cart;
    }
    
    @DELETE
    @Path("/items/{bookId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Cart removeItemFromCart(
            @PathParam("customerId") String customerId,
            @PathParam("bookId") String bookId) {
        
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Get cart
        Cart cart = cartStore.get(customerId);
        if (cart == null) {
            throw new CartNotFoundException("Cart for customer with ID " + customerId + " does not exist.");
        }
        
        // Remove item
        cart.removeItem(bookId);
        
        return cart;
    }
    
    // Utility methods
    private void validateCartItem(CartItem cartItem) {
        if (cartItem.getBookId() == null || cartItem.getBookId().trim().isEmpty()) {
            throw new InvalidInputException("Book ID cannot be empty.");
        }
        
        if (cartItem.getQuantity() <= 0) {
            throw new InvalidInputException("Quantity must be greater than 0.");
        }
    }
    
    // Helper method to get a cart, used by OrderResource
    public static Cart getCart(String customerId) {
        return cartStore.get(customerId);
    }
    
    // Helper method to remove a cart, used by OrderResource
    public static void removeCart(String customerId) {
        cartStore.remove(customerId);
    }
}
