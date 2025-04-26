/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bookstore.resource;

/**
 *
 * @author ASUS
 */
import com.bookstore.exception.CustomerNotFoundException;
import com.bookstore.exception.InvalidInputException;
import com.bookstore.models.Customer;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/customers")
public class CustomerResource {

    private static final ConcurrentHashMap<String, Customer> customerStore = new ConcurrentHashMap<>();
    
    // Initialize with some sample data
    static {
        Customer customer1 = new Customer(
                "1",
                "John Doe",
                "john.doe@example.com",
                "password123");
        
        Customer customer2 = new Customer(
                "2",
                "Jane Smith",
                "jane.smith@example.com",
                "password456");
        
        customerStore.put(customer1.getId(), customer1);
        customerStore.put(customer2.getId(), customer2);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customerStore.values());
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Customer getCustomerById(@PathParam("id") String id) {
        Customer customer = customerStore.get(id);
        if (customer == null) {
            throw new CustomerNotFoundException("Customer with ID " + id + " does not exist.");
        }
        return customer;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCustomer(Customer customer) {
        // Validation
        validateCustomer(customer);
        
        // Check if email already exists
        boolean emailExists = customerStore.values().stream()
                .anyMatch(c -> c.getEmail().equals(customer.getEmail()));
        
        if (emailExists) {
            throw new InvalidInputException("A customer with this email already exists.");
        }
        
        // Generate ID
        String id = UUID.randomUUID().toString();
        customer.setId(id);
        
        // Store the customer
        customerStore.put(id, customer);
        
        return Response.status(Response.Status.CREATED)
                .entity(customer)
                .build();
    }
    
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Customer updateCustomer(@PathParam("id") String id, Customer updatedCustomer) {
        // Check if customer exists
        Customer existingCustomer = customerStore.get(id);
        if (existingCustomer == null) {
            throw new CustomerNotFoundException("Customer with ID " + id + " does not exist.");
        }
        
        // Validation
        validateCustomer(updatedCustomer);
        
        // Check if updated email already exists (and it's not this customer's email)
        if (!existingCustomer.getEmail().equals(updatedCustomer.getEmail())) {
            boolean emailExists = customerStore.values().stream()
                    .anyMatch(c -> c.getEmail().equals(updatedCustomer.getEmail()) && !c.getId().equals(id));
            
            if (emailExists) {
                throw new InvalidInputException("A customer with this email already exists.");
            }
        }
        
        // Update customer
        updatedCustomer.setId(id);
        customerStore.put(id, updatedCustomer);
        
        return updatedCustomer;
    }
    
    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") String id) {
        Customer customer = customerStore.remove(id);
        if (customer == null) {
            throw new CustomerNotFoundException("Customer with ID " + id + " does not exist.");
        }
        
        return Response.status(Response.Status.NO_CONTENT).build();
    }
    
    // Utility methods
    private void validateCustomer(Customer customer) {
        List<String> validationErrors = new ArrayList<>();
        
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            validationErrors.add("Customer name cannot be empty.");
        }
        
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            validationErrors.add("Email cannot be empty.");
        } else if (!customer.getEmail().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            validationErrors.add("Email format is invalid.");
        }
        
        if (customer.getPassword() == null || customer.getPassword().trim().isEmpty()) {
            validationErrors.add("Password cannot be empty.");
        } else if (customer.getPassword().length() < 6) {
            validationErrors.add("Password must be at least 6 characters long.");
        }
        
        if (!validationErrors.isEmpty()) {
            throw new InvalidInputException(String.join(" ", validationErrors));
        }
    }
    
    // Helper method that can be used by other resources to check if a customer exists
    public static boolean customerExists(String customerId) {
        return customerStore.containsKey(customerId);
    }
    
    // Helper method to get a customer by ID, used by other resources
    public static Customer getCustomer(String customerId) {
        return customerStore.get(customerId);
    }
}
