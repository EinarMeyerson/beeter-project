package edu.upc.eetac.dsa.jgordo.beeter.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.jgordo.beeter.api.model.Sting;
import edu.upc.eetac.dsa.jgordo.beeter.api.model.StingCollection;
import edu.upc.eetac.dsa.jgordo.beeter.api.model.User;

@Path("/user/{username}")
public class UserResource {
	
	@Context
	private SecurityContext security;
	
	private void validateUser(String user) {
		
		System.out.println("Validacion del usuario: "+ user);
		
		User currentUser = getUserFromDatabase(user);
		
		System.out.println("Comparamos :" + currentUser.getUsername() + " con security: " + security.getUserPrincipal().getName());
		
		if (!security.getUserPrincipal().getName().equals(currentUser.getUsername())) throw new ForbiddenException(
					"You are not allowed to modify this sting.");
		
	}
	
    private DataSource ds = DataSourceSPA.getInstance().getDataSource();
	
	@GET
	@Produces(MediaType.BEETER_API_USER)
	public User getUser(@PathParam("username") String user) {
		
		User usuario = getUserFromDatabase(user);
		return usuario;
	}
	
	private String bulidGetUserQuery()
	{
		return "select * from users where username=?";
	}
	
	public User getUserFromDatabase (String username)
	{
		
		User dbuser = new User();
		Connection conn = null;
		
		try {
			conn = ds.getConnection();
			
		} 
		catch (SQLException e)
		{
			throw new ServerErrorException("Could not connect to the database",Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt = null;
		
		try {
			
			stmt = conn.prepareStatement(bulidGetUserQuery());
			
			System.out.println("Usuario es: " + username);
			stmt.setString(1,username);
			
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				
				dbuser.setUsername(rs.getString("username"));
				System.out.println("NombreUsuario: " + dbuser.getUsername());
				dbuser.setName(rs.getString("name"));
				System.out.println("Nombre: " + dbuser.getName());
				dbuser.setEmail(rs.getString("email"));
				System.out.println("Email: " + dbuser.getEmail());
				}
		} 
		catch (SQLException e)
		{
			
			throw new ServerErrorException(e.getMessage(),Response.Status.INTERNAL_SERVER_ERROR);
		} 
		finally 
		{
			try
			{
				if (stmt != null)
					stmt.close();
				conn.close();
			} 
			catch (SQLException e)
			{
				
			}
		}
		
		
	 System.out.println("Fin de getUserFromDatabase");
		return dbuser;
	}
	
	private String buildGetStingsQuery(String user) {
		if (user.equals(null))
			
			return "El usuario no puede ser nulo snitchi";
		
		else
			
			return "select * from users ";
	}
	
	
	@GET
	@Path("/stings")
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection getStings(@PathParam("username") String user,@QueryParam("length") int length,
			@QueryParam("before") long before, @QueryParam("after") long after) {
		System.out.println("Usuario es: " + user);
		StingCollection stings = new StingCollection();
	 
		Connection conn = null;
		try {
			conn = ds.getConnection();
			System.out.println("conexion realizada con exito");
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",Response.Status.SERVICE_UNAVAILABLE);
		}
	 
		PreparedStatement stmt = null;
		
		try {
			System.out.println("mandando la query");
			stmt = conn.prepareStatement(buildGetStings());
			System.out.println("insertand en la query");
			stmt.setString(1,  user);
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			long oldestTimestamp = 0;
			while (rs.next()) {
				System.out.println("reciviendo parametors");
				Sting sting = new Sting();
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString("content"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);
				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}
				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	 
		return stings;
	}
	
	private String buildGetStings()
	{
		return "select * from stings where username = ? ";
		
	}
	
	@PUT
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User updateSting(@PathParam("username") String username, User usermodify)
	{
		validateUser(username);
		System.out.println("Usuario a mnodificar: "+ username);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
	 
		PreparedStatement stmt = null;
		try {
			System.out.println("Comienza la query");
			String sql = buildUpdateUser();
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, usermodify.getName());
			stmt.setString(2, usermodify.getEmail());
			stmt.setString(3, username);
	 
			int rows = stmt.executeUpdate();
			System.out.println("Ejecuta la query");
			
			if (rows == 1){
				
				usermodify = getUserFromDatabase(username);
			    System.out.println("muestra la actualizacion");
			}
			else {
				throw new NotFoundException("There's no sting with stingid="
						+ usermodify);
			}
	 
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		
		System.out.println("Fin");
		return usermodify;
	}
	private String buildUpdateUser() {
		return "update users set name=ifnull(?, name), email=ifnull(?, email) where username=?";
	}
	/*
	@GET
	@Produces(MediaType.BEETER_API_USER)
	public Response GetUserList2(@PathParam("username") String username, @Context Request request)
	
	{
		//Esto sirve para cachear , para comprobar simplemente cojer el etaag y ponerlo en la el campo de cabeceras
		CacheControl cc = new CacheControl();
		User usuario = getUserFromDatabase(username);
		
		EntityTag eTag = new EntityTag((buildHashCode(usuario)));
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);
		
		if (rb != null) {
			return rb.cacheControl(cc).tag(eTag).build();
		}

		// If rb is null then either it is first time request; or resource is
		// modified
		// Get the updated representation and return with Etag attached to it
		rb = Response.ok(usuario).cacheControl(cc).tag(eTag);

		return rb.build();
	}
	private String buildHashCode(User user)
	{
		String s = user.getName() + "" + user.getEmail();
		
		return Long.toString(s.hashCode());
	}
	*/
	

}
