/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bookstore.resource;

/**
 *
 * @author ASUS
 */
import com.bookstore.exception.AuthorNotFoundException;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.InvalidInputException;
import com.bookstore.models.Book;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/books")
public class BookResource {

    private static final ConcurrentHashMap<String, Book> bookStore = new ConcurrentHashMap<>();
    
    // Initialize with some sample data
    static {
        Book book1 = new Book(
                UUID.randomUUID().toString(),
                "The Lord of the Rings",
                "1",  // Sample author ID
                "978-0618640157",
                1954,
                19.99,
                50);
        
        Book book2 = new Book(
                UUID.randomUUID().toString(),
                "To Kill a Mockingbird",
                "2",  // Sample author ID
                "978-0061120084",
                1960,
                14.99,
                75);
        
        Book book3 = new Book(
                UUID.randomUUID().toString(),
                "1984",
                "3",  // Sample author ID
                "978-0451524935",
                1949,
                12.99,
                60);
        
        bookStore.put(book1.getId(), book1);
        bookStore.put(book2.getId(), book2);
        bookStore.put(book3.getId(), book3);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> getAllBooks() {
        return new ArrayList<>(bookStore.values());
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book getBookById(@PathParam("id") String id) {
        Book book = bookStore.get(id);
        if (book == null) {
            throw new BookNotFoundException("Book with ID " + id + " does not exist.");
        }
        return book;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBook(Book book) {
        // Validation
        validateBook(book);
        
        // Check if author exists (in a real application, we would call AuthorResource)
        // This is a simplified version for demonstration
        if (book.getAuthorId() == null || book.getAuthorId().trim().isEmpty()) {
            throw new InvalidInputException("Author ID cannot be empty.");
        }
        
        // Check the author exists via AuthorResource
        AuthorResource authorResource = new AuthorResource();
        try {
            authorResource.getAuthorById(book.getAuthorId());
        } catch (AuthorNotFoundException e) {
            throw new AuthorNotFoundException("Author with ID " + book.getAuthorId() + " does not exist.");
        }
        
        // Generate ID
        String id = UUID.randomUUID().toString();
        book.setId(id);
        
        // Store the book
        bookStore.put(id, book);
        
        return Response.status(Response.Status.CREATED)
                .entity(book)
                .build();
    }
    
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Book updateBook(@PathParam("id") String id, Book updatedBook) {
        // Check if book exists
        Book existingBook = bookStore.get(id);
        if (existingBook == null) {
            throw new BookNotFoundException("Book with ID " + id + " does not exist.");
        }
        
        // Validation
        validateBook(updatedBook);
        
        // Check if author exists
        if (updatedBook.getAuthorId() != null && !updatedBook.getAuthorId().trim().isEmpty()) {
            AuthorResource authorResource = new AuthorResource();
            try {
                authorResource.getAuthorById(updatedBook.getAuthorId());
            } catch (AuthorNotFoundException e) {
                throw new AuthorNotFoundException("Author with ID " + updatedBook.getAuthorId() + " does not exist.");
            }
        } else {
            // Keep original author if not provided
            updatedBook.setAuthorId(existingBook.getAuthorId());
        }
        
        // Update book
        updatedBook.setId(id);
        bookStore.put(id, updatedBook);
        
        return updatedBook;
    }
    
    @DELETE
    @Path("/{id}")
    public Response deleteBook(@PathParam("id") String id) {
        Book book = bookStore.remove(id);
        if (book == null) {
            throw new BookNotFoundException("Book with ID " + id + " does not exist.");
        }
        
        return Response.status(Response.Status.NO_CONTENT).build();
    }
    
    // Utility methods
    private void validateBook(Book book) {
        List<String> validationErrors = new ArrayList<>();
        
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            validationErrors.add("Title cannot be empty.");
        }
        
        if (book.getAuthorId() == null || book.getAuthorId().trim().isEmpty()) {
            validationErrors.add("Author ID cannot be empty.");
        }
        
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            validationErrors.add("ISBN cannot be empty.");
        }
        
        if (book.getPublicationYear() > LocalDate.now().getYear()) {
            validationErrors.add("Publication year cannot be in the future.");
        }
        
        if (book.getPrice() < 0) {
            validationErrors.add("Price cannot be negative.");
        }
        
        if (book.getStock() < 0) {
            validationErrors.add("Stock cannot be negative.");
        }
        
        if (!validationErrors.isEmpty()) {
            throw new InvalidInputException(String.join(" ", validationErrors));
        }
    }
}
