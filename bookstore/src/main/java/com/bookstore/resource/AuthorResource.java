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
import com.bookstore.exception.InvalidInputException;
import com.bookstore.models.Author;
import com.bookstore.models.Book;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Path("/authors")
public class AuthorResource {

    private static final ConcurrentHashMap<String, Author> authorStore = new ConcurrentHashMap<>();
    
    // Initialize with some sample data
    static {
        Author author1 = new Author(
                "1",
                "J.R.R. Tolkien",
                "John Ronald Reuel Tolkien was an English writer, poet, philologist, and academic.");
        
        Author author2 = new Author(
                "2",
                "Harper Lee",
                "Nelle Harper Lee was an American novelist best known for her 1960 novel To Kill a Mockingbird.");
        
        Author author3 = new Author(
                "3",
                "George Orwell",
                "Eric Arthur Blair, known by his pen name George Orwell, was an English novelist, essayist, journalist, and critic.");
        
        authorStore.put(author1.getId(), author1);
        authorStore.put(author2.getId(), author2);
        authorStore.put(author3.getId(), author3);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Author> getAllAuthors() {
        return new ArrayList<>(authorStore.values());
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Author getAuthorById(@PathParam("id") String id) {
        Author author = authorStore.get(id);
        if (author == null) {
            throw new AuthorNotFoundException("Author with ID " + id + " does not exist.");
        }
        return author;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAuthor(Author author) {
        // Validation
        validateAuthor(author);
        
        // Generate ID if not provided
        if (author.getId() == null || author.getId().trim().isEmpty()) {
            String id = UUID.randomUUID().toString();
            author.setId(id);
        }
        
        // Store the author
        authorStore.put(author.getId(), author);
        
        return Response.status(Response.Status.CREATED)
                .entity(author)
                .build();
    }
    
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Author updateAuthor(@PathParam("id") String id, Author updatedAuthor) {
        // Check if author exists
        if (!authorStore.containsKey(id)) {
            throw new AuthorNotFoundException("Author with ID " + id + " does not exist.");
        }
        
        // Validation
        validateAuthor(updatedAuthor);
        
        // Update author
        updatedAuthor.setId(id);
        authorStore.put(id, updatedAuthor);
        
        return updatedAuthor;
    }
    
    @DELETE
    @Path("/{id}")
    public Response deleteAuthor(@PathParam("id") String id) {
        Author author = authorStore.remove(id);
        if (author == null) {
            throw new AuthorNotFoundException("Author with ID " + id + " does not exist.");
        }
        
        return Response.status(Response.Status.NO_CONTENT).build();
    }
    
    @GET
    @Path("/{id}/books")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> getBooksByAuthor(@PathParam("id") String authorId) {
        // Check if author exists
        if (!authorStore.containsKey(authorId)) {
            throw new AuthorNotFoundException("Author with ID " + authorId + " does not exist.");
        }
        
        // Get all books by this author
        BookResource bookResource = new BookResource();
        List<Book> allBooks = bookResource.getAllBooks();
        
        // Filter books by author ID
        return allBooks.stream()
                .filter(book -> authorId.equals(book.getAuthorId()))
                .collect(Collectors.toList());
    }
    
    // Utility methods
    private void validateAuthor(Author author) {
        List<String> validationErrors = new ArrayList<>();
        
        if (author.getName() == null || author.getName().trim().isEmpty()) {
            validationErrors.add("Author name cannot be empty.");
        }
        
        if (!validationErrors.isEmpty()) {
            throw new InvalidInputException(String.join(" ", validationErrors));
        }
    }
}
