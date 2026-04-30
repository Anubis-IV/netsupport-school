package anubis.netsupport_school.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.List;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner cmd(ServletWebServerApplicationContext context){
//		return (args) -> {
//			try{
//				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//				while (interfaces.hasMoreElements()) {
//					NetworkInterface iface = interfaces.nextElement();
//
//					// 1. Skip loopback, down, and virtual interfaces (like VMware/Docker)
//					if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
//
//					// 2. Additional filter for VMware/VirtualBox/Docker by name
//					String name = iface.getName().toLowerCase();
//					if (name.contains("vbox") || name.contains("vmnet") || name.contains("docker")) continue;
//
//					Enumeration<InetAddress> addresses = iface.getInetAddresses();
//					while (addresses.hasMoreElements()) {
//						InetAddress addr = addresses.nextElement();
//						// 3. Only look for IPv4 and skip the local loopback
//						if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
//							System.out.println("server ip:" + addr.getHostAddress());
//							System.out.println("host name:" + addr.getCanonicalHostName());
//							System.out.println("iface name:" + iface.getName());
//						}
//					}
//				}
//			}catch (Exception e){
//				System.out.println("server IP not found");
//			}
//
//
//			System.out.println("port number: " + context.getWebServer().getPort());
//		};
//	}

	@Bean
	public CommandLineRunner sqliteConfig(DataSource dataSource) {
		return args -> {
			try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

				stmt.execute("PRAGMA journal_mode=WAL;");
				stmt.execute("PRAGMA busy_timeout=5000;"); // 5 seconds wait
			}
		};
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins("*")
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
						.allowedHeaders("*")
						.maxAge(3600);
			}
		};
	}


}
