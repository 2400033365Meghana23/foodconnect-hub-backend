package com.foodwaste.platform.service;

import com.foodwaste.platform.exception.ApiException;
import com.foodwaste.platform.security.CurrentUser;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public void requireRoles(CurrentUser user, String... roles) {
        if (user == null) {
            throw new ApiException(401, "Unauthorized");
        }
        Set<String> allowed = Set.of(roles);
        if (!allowed.contains(user.getRole())) {
            throw new ApiException(403, "Forbidden for this role");
        }
    }
}
