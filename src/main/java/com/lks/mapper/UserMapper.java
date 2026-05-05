package com.lks.mapper;

import com.lks.bean.User;
import com.lks.bean.RecoveryToken;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;
import java.util.List;

public interface UserMapper {

	// Fetch all users
	@Select("SELECT * FROM t_user")
	List<User> userList();

	// Fetch users based on specific conditions
	List<User> getListByUser(User user);
	
	@Select("SELECT COUNT(*) FROM t_user")
    long getTotalUserCount();

	// Login user by username and password
	@Select("SELECT * FROM t_user WHERE username = #{username} AND password = #{password}")
	User loginUser(User user);

	// Delete user by ID
	@Delete("DELETE FROM t_user WHERE id = #{id}")
	Integer deleteUser(@Param("id") Integer id);

	// Insert a new user into the database
	@Insert("INSERT INTO t_user(fullName, username, password, email, description) VALUES (#{fullName}, #{username}, #{password}, #{email}, #{description})")
	Integer saveUser(User user);

	// Update the user's information including lock time
	@Update("UPDATE t_user SET fullName=#{fullName}, username=#{username}, password=#{password}, email=#{email}, description=#{description}, lock_time=#{lockTime} WHERE id=#{id}")
	Integer updateUser(User user);

	// Find a user by their username
	@Select("SELECT * FROM t_user WHERE username = #{username}")
	User findByUsername(String username);

	// Find a user by their email
	@Select("SELECT * FROM t_user WHERE email = #{email}")
	User findByEmail(String email);

	// Insert a new recovery token for a user
	@Insert("INSERT INTO t_user_recovery_tokens (user_id, token_hash, expires_at) VALUES (#{userId}, #{tokenHash}, #{expiresAt})")
	int insertRecoveryToken(@Param("userId") Integer userId, @Param("tokenHash") String tokenHash,
			@Param("expiresAt") Timestamp expiresAt);

	// Find a recovery token by token hash and check its validity
	@Select("SELECT * FROM t_user_recovery_tokens WHERE token_hash = #{tokenHash} AND expires_at > NOW() AND is_used = FALSE LIMIT 1")
	RecoveryToken findByRecoveryToken(@Param("tokenHash") String tokenHash);

	// Update the token to mark it as used
	@Update("UPDATE t_user_recovery_tokens SET is_used = TRUE WHERE id = #{id}")
	int markTokenAsUsed(@Param("id") Integer id);

	// Increment failed attempts for a token
	@Update("UPDATE t_user_recovery_tokens SET failed_attempts = failed_attempts + 1 WHERE id = #{id}")
	int incrementFailedAttempts(@Param("id") Integer id);

	// Find user by ID
	@Select("SELECT * FROM t_user WHERE id = #{id}")
	User findById(@Param("id") Integer id);

	// SQL method to retrieve active tokens that have not expired and are not marked
	// as used
	@Select("SELECT * FROM t_user_recovery_tokens WHERE expires_at > NOW() AND is_used = FALSE")
	List<RecoveryToken> findActiveRecoveryTokens();

	// Method to count recent recovery requests by the user's email within the last
	// 15 minutes
	@Select("SELECT COUNT(*) FROM t_user_recovery_tokens WHERE user_id = #{userId} AND created_at > NOW() - INTERVAL 15 MINUTE")
	int countRecentRecoveryAttempts(@Param("userId") Integer userId);

	// SQL method to find an existing active token that hasn't expired and hasn't
	// been used yet
	@Select("SELECT * FROM t_user_recovery_tokens WHERE user_id = #{userId} AND expires_at > NOW() AND is_used = FALSE LIMIT 1")
	RecoveryToken findActiveTokenByUserId(@Param("userId") Integer userId);

	// Clear all recovery tokens for the user after a successful password change
	@Delete("DELETE FROM t_user_recovery_tokens WHERE user_id = #{userId}")
	int clearAllRecoveryTokensForUser(@Param("userId") Integer userId);
}