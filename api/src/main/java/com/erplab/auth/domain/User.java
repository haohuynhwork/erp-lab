package com.erplab.auth.domain;

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

	private String username;

	private String passwordHash;

	private String fullName;

	private String email;

	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private Set<Role> roles = EnumSet.noneOf(Role.class);

	protected User() {
	}

	public User(String username, String passwordHash, String fullName, String email, Set<Role> roles) {
		this.username = username.trim().toLowerCase();
		this.passwordHash = passwordHash;
		update(fullName, email, roles, true);
	}

	public void update(String fullName, String email, Set<Role> roles, boolean enabled) {
		if (roles == null || roles.isEmpty()) {
			throw new DomainException("roles_required", "A user needs at least one role");
		}
		this.fullName = fullName;
		this.email = email;
		this.roles = EnumSet.copyOf(roles);
		this.enabled = enabled;
	}

	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void delete() {
		if (roles.contains(Role.ADMIN) && username.equals("admin")) {
			throw new DomainException("cannot_delete_admin", "The built-in admin cannot be deleted");
		}
		enabled = false;
		markDeleted();
	}

	public boolean canSignIn() {
		return enabled && !isDeleted();
	}

	public Set<String> permissions() {
		var perms = new TreeSet<String>();
		roles.forEach(r -> perms.addAll(r.permissions()));
		return perms;
	}

}
