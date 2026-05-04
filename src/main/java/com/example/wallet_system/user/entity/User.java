package com.example.wallet_system.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
 
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
 
/**
 * User entity — implements UserDetails để Spring Security dùng trực tiếp.
 *
 * Q: Tại sao implement UserDetails ở đây thay vì tạo class wrapper riêng?
 * A: Với project quy mô này, gộp vào cho gọn. Nếu sau này User có nhiều role
 *    phức tạp hơn thì nên tách ra CustomUserDetails riêng.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true)
    private String email;
 
    @Column(nullable = false)
    private String password;   // bcrypt hash
 
    @Column(name = "full_name", nullable = false)
    private String fullName;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
 
    // ===== UserDetails overrides =====
    // Q: getUsername() trả về email thay vì username là sao?
    // A: App này dùng email làm định danh, Spring Security chỉ cần
    //    getUsername() trả về unique identifier — không nhất thiết phải là "username".
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // Chưa có role system, để trống
    }
 
    @Override
    public String getUsername() {
        return email;
    }
 
    @Override
    public boolean isAccountNonExpired()  { return true; }
 
    @Override
    public boolean isAccountNonLocked()   { return true; }
 
    @Override
    public boolean isCredentialsNonExpired() { return true; }
 
    @Override
    public boolean isEnabled()            { return true; }
}
