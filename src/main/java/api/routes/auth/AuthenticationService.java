package api.routes.auth;

import api.auth.JwtService;
import api.auth.Role;
import api.auth.UserModel;
import api.routes.auth.dto.LoginDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public AuthenticationResponse login(LoginDto dto) throws Exception {
        final var validUserDetails = userDetailsService.loadUserByUsername(dto.getUsername());

        final var user = UserModel.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.USER)
                .build();

        if (!validUserDetails.getPassword().equals(dto.getPassword()))
            throw new Exception("Invalid password");

        return AuthenticationResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }
}
