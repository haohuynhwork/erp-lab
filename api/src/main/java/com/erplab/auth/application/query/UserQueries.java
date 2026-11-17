package com.erplab.auth.application.query;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.auth.domain.Role;
import com.erplab.auth.domain.User;
import com.erplab.auth.domain.UserRepository;
import com.erplab.common.domain.NotFoundException;
import com.erplab.common.http.PageResult;

@Service
@Transactional(readOnly = true)
public class UserQueries {

	public record UserView(Long id, String username, String fullName, String email, boolean enabled, Set<Role> roles,
			Set<String> permissions) {

		static UserView of(User u) {
			return new UserView(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.isEnabled(),
					u.getRoles(), u.permissions());
		}

	}

	private final UserRepository users;

	UserQueries(UserRepository users) {
		this.users = users;
	}

	public PageResult<UserView> list(int page, int size) {
		var all = users.findByDeletedAtIsNull(PageRequest.of(page, size, Sort.by("username")));
		return PageResult.of(all, UserView::of);
	}

	public UserView get(Long id) {
		return users.findById(id).filter(u -> !u.isDeleted()).map(UserView::of)
			.orElseThrow(() -> new NotFoundException("User", id));
	}

	public UserView byUsername(String username) {
		return users.findByUsernameAndDeletedAtIsNull(username).map(UserView::of)
			.orElseThrow(() -> new NotFoundException("User", username));
	}

}
