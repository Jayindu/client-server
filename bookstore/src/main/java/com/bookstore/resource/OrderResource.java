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
import com.bookstore.exception.CustomerNotFoundException;
import com.bookstore.exception.InvalidInputException;
import com.bookstore.exception.OutOfStockException;
import com.bookstore.models.Book;
import com.bookstore.models.Cart;
import com.bookstore.models.CartItem;
import com.bookstore.models.Order;
import com.bookstore.models.OrderItem;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/customers/{customerId}/orders")
public class OrderResource {

    private static final ConcurrentHashMap<String, List<Order>> orderStore = new ConcurrentHashMap<>();
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(@PathParam("customerId") String customerId) {
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Get customer's cart
        Cart cart = CartResource.getCart(customerId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new InvalidInputException("Customer's cart is empty. Cannot create order.");
        }
        
        // Check stock for all items
        BookResource bookResource = new BookResource();
        List<String> outOfStockItems = new ArrayList<>();
        
        for (CartItem cartItem : cart.getItems()) {
            Book book;
            try {
                book = bookResource.getBookById(cartItem.getBookId());
            } catch (BookNotFoundException e) {
                throw new BookNotFoundException("Book with ID " + cartItem.getBookId() + " does not exist.");
            }
            
            if (book.getStock() < cartItem.getQuantity()) {
                outOfStockItems.add("Book: " + book.getTitle() + ", Available: " + book.getStock() + ", Requested: " + cartItem.getQuantity());
            }
        }
        
        if (!outOfStockItems.isEmpty()) {
            throw new OutOfStockException("Some items are out of stock: " + String.join(", ", outOfStockItems));
        }
        
        // Create new order
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, customerId);
        
        // Convert cart items to order items
        for (CartItem cartItem : cart.getItems()) {
            Book book = bookResource.getBookById(cartItem.getBookId());
            
            // Update book stock
            book.setStock(book.getStock() - cartItem.getQuantity());
            
            // Create order item
            OrderItem orderItem = new OrderItem(
                    cartItem.getBookId(),
                    book.getTitle(),
                    cartItem.getQuantity(),
                    cartItem.getPrice()
            );
            
            order.addItem(orderItem);
        }
        
        // Store order
        orderStore.computeIfAbsent(customerId, k -> new ArrayList<>()).add(order);
        
        // Clear customer's cart
        CartResource.removeCart(customerId);
        
        return Response.status(Response.Status.CREATED)
                .entity(order)
                .build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Order> getCustomerOrders(@PathParam("customerId") String customerId) {
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Get orders for customer
        List<Order> orders = orderStore.getOrDefault(customerId, new ArrayList<>());
        
        return orders;
    }
    
    @GET
    @Path("/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Order getOrderById(
            @PathParam("customerId") String customerId,
            @PathParam("orderId") String orderId) {
        
        // Check if customer exists
        if (!CustomerResource.customerExists(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " does not exist.");
        }
        
        // Get orders for customer
        List<Order> orders = orderStore.getOrDefault(customerId, new ArrayList<>());
        
        // Find order by ID
        Order order = orders.stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElse(null);
        
        if (order == null) {
            throw new InvalidInputException("Order with ID " + orderId + " not found for this customer.");
        }
        
        return order;
    }
}
