package rbac;

public record Permission(String name, String resource, String description) {

    public Permission {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name не может быть null или пустым");
        }
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("resource не может быть null или пустым");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description не может быть null или пустым");
        }

        name = name.trim().toUpperCase().replaceAll("\\s+", "");    
        resource = resource.trim().toLowerCase();                    
        description = description.trim();                         

        if (name.isEmpty()) {
            throw new IllegalArgumentException("После нормализации name стал пустым");
        }
        if (resource.isEmpty()) {
            throw new IllegalArgumentException("После нормализации resource стал пустым");
        }
    }

    public String format() {
        return name + " on " + resource + ": " + description;
    }

    public boolean matches(String namePattern, String resourcePattern) {
        if (namePattern == null || resourcePattern == null) {
            return false;
        }

        String upperPattern = namePattern.toUpperCase();
        String lowerResourcePattern = resourcePattern.toLowerCase();

        return name.contains(upperPattern) &&
               resource.contains(lowerResourcePattern);
    }
}