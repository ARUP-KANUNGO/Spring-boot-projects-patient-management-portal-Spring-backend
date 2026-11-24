export function validateRequired(value) {
    return value !== undefined && value !== null && value.trim() !== '';
}

export function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(String(email).toLowerCase());
}

export function validateMinLength(value, minLength) {
    return value && value.length >= minLength;
}

export function validateLogin(username, password) {
    const errors = [];
    if (!validateRequired(username)) {
        errors.push({ field: 'username', message: 'Username is required' });
    }
    if (!validateRequired(password)) {
        errors.push({ field: 'password', message: 'Password is required' });
    }
    return errors;
}

export function validateRegistration(data) {
    const errors = [];
    
    if (!validateRequired(data.fullName)) errors.push({ field: 'fullName', message: 'Full name is required' });
    
    if (!validateRequired(data.email)) {
        errors.push({ field: 'email', message: 'Email is required' });
    } else if (!validateEmail(data.email)) {
        errors.push({ field: 'email', message: 'Please enter a valid email address' });
    }
    
    if (!validateRequired(data.username)) {
        errors.push({ field: 'username', message: 'Username is required' });
    } else if (!validateMinLength(data.username, 3)) {
        errors.push({ field: 'username', message: 'Username must be at least 3 characters long' });
    } else if (!/^[A-Za-z0-9._-]+$/.test(data.username)) {
        errors.push({ field: 'username', message: 'Username may only contain letters, digits, dots, underscores and hyphens' });
    }
    
    if (!validateRequired(data.password)) {
        errors.push({ field: 'password', message: 'Password is required' });
    } else if (!validateMinLength(data.password, 8)) {
        errors.push({ field: 'password', message: 'Password must be at least 8 characters long' });
    } else if (!/[A-Za-z]/.test(data.password) || !/\d/.test(data.password)) {
        errors.push({ field: 'password', message: 'Password must contain at least one letter and one digit' });
    }
    
    if (data.password !== data.confirmPassword) {
        errors.push({ field: 'confirmPassword', message: 'Passwords do not match' });
    }
    
    return errors;
}
