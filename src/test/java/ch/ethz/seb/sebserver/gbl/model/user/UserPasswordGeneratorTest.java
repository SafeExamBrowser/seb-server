package ch.ethz.seb.sebserver.gbl.model.user;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserPasswordGeneratorTest {

    @Test
    public void printPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(8);

        //Type in password here and run the test. It should output a hashed password
        String rawPassword = "insert password here";
        String hash = encoder.encode(rawPassword);

        System.out.println("RAW:  " + rawPassword);
        System.out.println("HASH: " + hash);
    }
}