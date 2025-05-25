package com.movie.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import com.movie.bus.RoomBUS;
import com.movie.model.Room;
import javax.swing.*;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import com.movie.bus.RoomBUS;
import com.movie.model.Room;
import javax.swing.table.*;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Font;
import com.movie.bus.MovieBUS;
import com.movie.bus.ShowtimeBUS;
import com.movie.bus.StaffBUS;
import com.movie.bus.CustomerBUS;
import com.movie.bus.TicketBUS;
import com.movie.model.Movie;
import com.movie.model.Showtime;
import com.movie.model.Staff;
import com.movie.model.Customer;
import com.movie.dao.MovieDAO;
import com.movie.dao.RevenueDAO;
import com.movie.dao.ShowtimeDAO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.*;
import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class AdminFrame extends JFrame {
    private JPanel mainPanel;
    private JPanel showtimeParentPanel;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private final MovieBUS movieBUS = new MovieBUS();
    private final RoomBUS roomBUS = new RoomBUS();
    private final ShowtimeBUS showtimeBUS = new ShowtimeBUS();
    private final StaffBUS staffBUS = new StaffBUS();
    private final CustomerBUS customerBUS = new CustomerBUS();
    private final TicketBUS ticketBUS = new TicketBUS();
    private final MovieDAO movieDAO = new MovieDAO();
    private final RevenueDAO revenueDAO = new RevenueDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JTextField durationField;
    private JTextField directorField;
    private JTextField genreField;
    private JTextField posterField;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField productionYearField;
    private JTextField countryField;
    private JTextField ageRestrictionField;
    private JLabel posterPreview;
    private JButton choosePosterButton;
    private JButton selectGenreButton;
    private JButton selectCountryButton;
    private JButton selectStartDateButton;
    private JButton selectEndDateButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JPanel movieListPanel;
    private Movie selectedMovie;
    private JPanel formPanel;
    private JTable roomTable;
    private JLabel timeLabel;
    private DefaultTableModel tableModel;
    private volatile boolean running = true; // Flag to control threads
    private JPanel showtimeListPanel;
    private List<Showtime> showtimes;
    private JPanel mainContent;
    public AdminFrame() {
        customizeGlobalUI();
        initUI();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        startClock();
        startShowtimeStatusUpdater();
    }

    private void customizeGlobalUI() {
        // Tùy chỉnh giao diện cho các nút trong JOptionPane
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.background", new Color(75, 192, 192)); // Xanh lam cho nút OK, Yes, No
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Loại bỏ viền mặc định, thay bằng viền mỏng
        UIManager.put("Button.focus", false); // Loại bỏ viền focus
        UIManager.put("Button.margin", new Insets(0, 0, 0, 0)); // Loại bỏ padding thừa
        UIManager.put("Button.arc", 10); // Thêm viền bo góc (nếu hỗ trợ, tùy thuộc theme)

        // Tùy chỉnh font và màu cho các thành phần khác của JOptionPane
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("OptionPane.background", new Color(245, 245, 245));
        UIManager.put("Panel.background", new Color(245, 245, 245));

        // Tùy chỉnh font mặc định cho các thành phần khác
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
    }
    private void initUI() {
        setTitle("Quản lý bán vé xem phim - Hiếu");
        setSize(1200, 800);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Stop threads when the frame is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false; // Stop threads
            }
        });

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel mainView = createMainView();
        mainPanel.add(mainView, "MainView");

        add(mainPanel);
        setVisible(true);

        // ❗❗ Gọi load suất chiếu và hẹn giờ cập nhật mỗi 30 giây
        loadShowtimes(); // Thay vì loadShowtimes(mainContent)
        new javax.swing.Timer(30000, e -> loadShowtimes()).start();
    }

    private void startClock() {
        timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(Color.BLACK);
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timePanel.add(timeLabel);
        add(timePanel, BorderLayout.NORTH);

        new Thread(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            while (running) {
                timeLabel.setText(sdf.format(new java.util.Date()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.err.println("Clock thread interrupted: " + e.getMessage());
                }
            }
        }).start();
    }

    private void startShowtimeStatusUpdater() {
        new Thread(() -> {
            while (running) {
                try {
                    showtimeBUS.getAllShowtimes();
                    Thread.sleep(60000);
                } catch (SQLException e) {
                    System.err.println("Error updating showtime status: " + e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Showtime updater thread interrupted: " + e.getMessage());
                }
            }
        }).start();
    }

    private JPanel createMainView() {
        JPanel mainView = new JPanel(new BorderLayout());
        mainView.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(40, 40, 40));

        JButton homeButton = new JButton("Trang chủ");
        JButton infoButton = new JButton("Thông tin phim");
        JButton roomButton = new JButton("Phòng chiếu");
        JButton showtimeButton = new JButton("Suất chiếu");
        JButton staffButton = new JButton("Nhân viên");
        JButton customerButton = new JButton("Khách hàng");
        JButton statsButton = new JButton("Thống kê");
        JButton logoutButton = new JButton("Đăng xuất");

        styleButton(homeButton);
        styleButton(infoButton);
        styleButton(roomButton);
        styleButton(showtimeButton);
        styleButton(staffButton);
        styleButton(customerButton);
        styleButton(statsButton);
        styleButton(logoutButton);

        homeButton.addActionListener(e -> showPanel("Trang chủ"));
        infoButton.addActionListener(e -> showPanel("Thông tin phim"));
        roomButton.addActionListener(e -> showPanel("Phòng chiếu"));
        showtimeButton.addActionListener(e -> showPanel("Suất chiếu"));
        staffButton.addActionListener(e -> showPanel("Nhân viên"));
        customerButton.addActionListener(e -> showPanel("Khách hàng"));
        statsButton.addActionListener(e -> showPanel("Thống kê"));
        logoutButton.addActionListener(e -> {
            running = false; // Stop threads before logout
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });

        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(homeButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(infoButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(roomButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(showtimeButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(staffButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(customerButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(statsButton);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(logoutButton);

        contentPanel = new JPanel(new CardLayout());
        contentPanel.add(createHomePanel(), "Trang chủ");
        contentPanel.add(createInfoPanel(), "Thông tin phim");
        contentPanel.add(createRoomPanel(), "Phòng chiếu");
        contentPanel.add(createShowtimePanel(), "Suất chiếu");
        contentPanel.add(createStaffPanel(), "Nhân viên");
        contentPanel.add(createCustomerPanel(), "Khách hàng");
        contentPanel.add(createStatsPanel(), "Thống kê");

        mainView.add(sidebar, BorderLayout.WEST);
        mainView.add(contentPanel, BorderLayout.CENTER);

        return mainView;
    }

    private void styleButton(JButton button) {
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(80, 80, 80));
            }

            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(60, 60, 60));
            }
        });
    }

    private void showPanel(String panelName) {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, panelName);
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Chào mừng đến với hệ thống quản lý bán vé xem phim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        infoArea.setText("Hệ thống này cho phép quản lý:\n" +
                "- Danh sách phim và thông tin chi tiết\n" +
                "- Phòng chiếu và suất chiếu\n" +
                "- Nhân viên phụ trách suất chiếu\n" +
                "- Thông tin khách hàng và lịch sử đặt vé\n" +
                "- Thống kê doanh thu theo ngày\n\n" +
                "Vui lòng chọn chức năng từ menu bên trái để bắt đầu.");
        infoArea.setBackground(new Color(245, 245, 245));
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);

        return panel;
    }
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Thông tin phim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainContent.setBackground(new Color(245, 245, 245));

        movieListPanel = new JPanel();
        movieListPanel.setLayout(new BoxLayout(movieListPanel, BoxLayout.Y_AXIS));
        JScrollPane movieScrollPane = new JScrollPane(movieListPanel);
        movieScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainContent.add(movieScrollPane, BorderLayout.CENTER);

        // Thêm form chỉnh sửa
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Chi tiết phim"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        titleField = new JTextField(20);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        durationField = new JTextField(10);
        directorField = new JTextField(20);
        genreField = new JTextField(20);
        posterField = new JTextField(20);
        startDateField = new JTextField(10);
        endDateField = new JTextField(10);
        productionYearField = new JTextField(10);
        countryField = new JTextField(20);
        ageRestrictionField = new JTextField(10);
        posterPreview = new JLabel();
        posterPreview.setPreferredSize(new Dimension(150, 200));
        posterPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        choosePosterButton = new JButton("Chọn poster");
        selectGenreButton = new JButton("Chọn thể loại");
        selectCountryButton = new JButton("Chọn quốc gia");
        selectStartDateButton = new JButton("Chọn ngày bắt đầu");
        selectEndDateButton = new JButton("Chọn ngày kết thúc");

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Tên phim:"), gbc);
        gbc.gridx = 1;
        formPanel.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Thời lượng (phút):"), gbc);
        gbc.gridx = 1;
        formPanel.add(durationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Đạo diễn:"), gbc);
        gbc.gridx = 1;
        formPanel.add(directorField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Thể loại:"), gbc);
        gbc.gridx = 1;
        formPanel.add(genreField, gbc);
        gbc.gridx = 2;
        formPanel.add(selectGenreButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Áp phích:"), gbc);
        gbc.gridx = 1;
        formPanel.add(posterField, gbc);
        gbc.gridx = 2;
        formPanel.add(choosePosterButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        formPanel.add(startDateField, gbc);
        gbc.gridx = 2;
        formPanel.add(selectStartDateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        formPanel.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        formPanel.add(endDateField, gbc);
        gbc.gridx = 2;
        formPanel.add(selectEndDateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        formPanel.add(new JLabel("Năm sản xuất:"), gbc);
        gbc.gridx = 1;
        formPanel.add(productionYearField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        formPanel.add(new JLabel("Quốc gia:"), gbc);
        gbc.gridx = 1;
        formPanel.add(countryField, gbc);
        gbc.gridx = 2;
        formPanel.add(selectCountryButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
        formPanel.add(new JLabel("Giới hạn tuổi:"), gbc);
        gbc.gridx = 1;
        formPanel.add(ageRestrictionField, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 11;
        formPanel.add(posterPreview, gbc);

        formPanel.setVisible(false);
        mainContent.add(formPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addButton = new JButton("Thêm phim");
        updateButton = new JButton("Cập nhật");
        updateButton.setEnabled(false);
        deleteButton = new JButton("Xóa");
        deleteButton.setEnabled(false);

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
        addButton.setFont(buttonFont);
        addButton.setForeground(Color.WHITE);
        addButton.setBackground(new Color(34, 139, 34)); // Xanh lá
        addButton.setFocusPainted(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        updateButton.setFont(buttonFont);
        updateButton.setForeground(Color.WHITE);
        updateButton.setBackground(new Color(75, 192, 192)); // Xanh dương nhạt
        updateButton.setFocusPainted(false);
        updateButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        deleteButton.setFont(buttonFont);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBackground(new Color(255, 99, 71)); // Đỏ
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        mainContent.add(buttonPanel, BorderLayout.SOUTH);

        loadMovies(movieListPanel);

        addButton.addActionListener(e -> showAddMovieDialog(null));
        updateButton.addActionListener(e -> updateMovie(null));
        deleteButton.addActionListener(e -> deleteMovie(null));
        choosePosterButton.addActionListener(e -> choosePoster());
        selectGenreButton.addActionListener(e -> selectGenre());
        selectCountryButton.addActionListener(e -> selectCountry());
        selectStartDateButton.addActionListener(e -> selectDate(startDateField));
        selectEndDateButton.addActionListener(e -> selectDate(endDateField));

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private void loadMovies(JPanel movieListPanel) {
        if (movieListPanel == null) return;

        JLabel loadingLabel = new JLabel("Đang tải danh sách phim...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));

        movieListPanel.removeAll();
        movieListPanel.add(loadingLabel);
        movieListPanel.revalidate();
        movieListPanel.repaint();

        new SwingWorker<List<Movie>, Void>() {
            @Override
            protected List<Movie> doInBackground() throws SQLException {
                try {
                    List<Movie> movies = movieBUS.getAllMovies();
                    // Debug: Kiểm tra dữ liệu trả về
                    System.out.println("Số lượng phim tải được: " + movies.size());
                    for (Movie movie : movies) {
                        System.out.println("Phim: " + movie.getTitle() + " | Mô tả: " + movie.getDescription());
                    }
                    return movies;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return new ArrayList<>();
                }
            }
            @Override
            protected void done() {
                try {
                    List<Movie> movies = get();
                    movieListPanel.removeAll();
                    if (movies.isEmpty()) {
                        JLabel noMoviesLabel = new JLabel("Hiện không có phim nào.", SwingConstants.CENTER);
                        noMoviesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                        movieListPanel.add(noMoviesLabel);
                    } else {
                        for (Movie movie : movies) {
                            JPanel moviePanel = createMoviePanel(movie);
                            movieListPanel.add(moviePanel);
                            movieListPanel.add(Box.createVerticalStrut(10));
                        }
                    }
                    movieListPanel.revalidate();
                    movieListPanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            AdminFrame.this,
                            "Không thể tải danh sách phim: " + ex.getMessage(),
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                    movieListPanel.removeAll();
                    JLabel errorLabel = new JLabel("Lỗi tải dữ liệu. Hãy thử lại.", SwingConstants.CENTER);
                    errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    movieListPanel.add(errorLabel);
                    movieListPanel.revalidate();
                    movieListPanel.repaint();
                }
            }
        }.execute();
    }

    private JPanel createMoviePanel(Movie movie) {
        JPanel moviePanel = new JPanel(new BorderLayout());
        moviePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        moviePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        moviePanel.setBackground(Color.WHITE);

        // Poster
        JPanel posterPanel = new JPanel(new BorderLayout());
        posterPanel.setPreferredSize(new Dimension(200, 140));
        posterPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel posterLabel = new JLabel();
        posterLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (movie.getPoster() != null && !movie.getPoster().isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(movie.getPoster());
                if (icon.getIconWidth() > 0) {
                    posterLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(180, 130, Image.SCALE_SMOOTH)));
                } else {
                    posterLabel.setText("Không có ảnh");
                    posterLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                }
            } catch (Exception e) {
                posterLabel.setText("Lỗi tải ảnh");
                posterLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                System.err.println("Error loading poster for " + (movie.getTitle() != null ? movie.getTitle() : "unknown") + ": " + e.getMessage());
            }
        } else {
            posterLabel.setText("Không có ảnh");
            posterLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        }

        posterPanel.add(posterLabel, BorderLayout.CENTER);
        moviePanel.add(posterPanel, BorderLayout.WEST);

        // Thông tin phim
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        infoPanel.setBackground(Color.WHITE);

        // Tên phim
        String title = movie.getTitle() != null && !movie.getTitle().isEmpty() ? movie.getTitle() : "Không có tên";
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(10));

        // Nội dung phim
        String description = movie.getDescription() != null && !movie.getDescription().isEmpty() ? movie.getDescription() : "Không có mô tả";
        JTextArea descriptionAreaLocal = new JTextArea(description);
        descriptionAreaLocal.setLineWrap(true);
        descriptionAreaLocal.setWrapStyleWord(true);
        descriptionAreaLocal.setEditable(false);
        descriptionAreaLocal.setBackground(Color.WHITE);
        descriptionAreaLocal.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionAreaLocal.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionAreaLocal.setRows(4); // Tăng số dòng để hiển thị rõ hơn

        JScrollPane descScrollPane = new JScrollPane(descriptionAreaLocal);
        descScrollPane.setBorder(BorderFactory.createEmptyBorder());
        descScrollPane.setPreferredSize(new Dimension(600, 80)); // Tăng chiều cao để hiển thị đầy đủ
        infoPanel.add(descScrollPane);

        moviePanel.add(infoPanel, BorderLayout.CENTER);

        moviePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedMovie = movie;
                formPanel.setVisible(true);
                updateButton.setEnabled(true);
                deleteButton.setEnabled(true);
                titleField.setText(selectedMovie.getTitle() != null ? selectedMovie.getTitle() : "");
                descriptionArea.setText(selectedMovie.getDescription() != null ? selectedMovie.getDescription() : "");
                durationField.setText(String.valueOf(selectedMovie.getDuration()));
                directorField.setText(selectedMovie.getDirector() != null ? selectedMovie.getDirector() : "");
                genreField.setText(selectedMovie.getGenreName() != null ? selectedMovie.getGenreName() : "");
                posterField.setText(selectedMovie.getPoster() != null ? selectedMovie.getPoster() : "");
                startDateField.setText(selectedMovie.getStartDate() != null ? selectedMovie.getStartDate().toString() : "");
                endDateField.setText(selectedMovie.getEndDate() != null ? selectedMovie.getEndDate().toString() : "");
                productionYearField.setText(String.valueOf(selectedMovie.getProductionYear()));
                countryField.setText(selectedMovie.getCountryName() != null ? selectedMovie.getCountryName() : "");
                ageRestrictionField.setText(String.valueOf(selectedMovie.getAgeRestriction()));
                if (selectedMovie.getPoster() != null && !selectedMovie.getPoster().isEmpty()) {
                    posterPreview.setIcon(new ImageIcon(new ImageIcon(selectedMovie.getPoster()).getImage().getScaledInstance(150, 200, Image.SCALE_SMOOTH)));
                } else {
                    posterPreview.setIcon(null);
                }
                titleField.setEditable(true);
                descriptionArea.setEditable(true);
                durationField.setEditable(true);
                directorField.setEditable(true);
                genreField.setEditable(false);
                posterField.setEditable(false);
                startDateField.setEditable(false);
                endDateField.setEditable(false);
                productionYearField.setEditable(true);
                countryField.setEditable(false);
                ageRestrictionField.setEditable(true);
                choosePosterButton.setEnabled(true);
                selectGenreButton.setEnabled(true);
                selectCountryButton.setEnabled(true);
                selectStartDateButton.setEnabled(true);
                selectEndDateButton.setEnabled(true);
            }
        });

        return moviePanel;
    }

    private void choosePoster() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String posterPath = selectedFile.getAbsolutePath();
            posterField.setText(posterPath);
            posterPreview.setIcon(new ImageIcon(new ImageIcon(posterPath).getImage().getScaledInstance(150, 200, Image.SCALE_SMOOTH)));
        }
    }

    private void selectGenre() {
        try {
            List<String> genres = movieBUS.getAllGenres();
            String selectedGenre = (String) JOptionPane.showInputDialog(
                    this, "Chọn thể loại:", "Lựa chọn thể loại",
                    JOptionPane.PLAIN_MESSAGE, null, genres.toArray(), genres.get(0));
            if (selectedGenre != null) {
                genreField.setText(selectedGenre);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách thể loại: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectCountry() {
        try {
            List<String> countries = movieBUS.getAllCountries();
            String selectedCountry = (String) JOptionPane.showInputDialog(
                    this, "Chọn quốc gia:", "Lựa chọn quốc gia",
                    JOptionPane.PLAIN_MESSAGE, null, countries.toArray(), countries.get(0));
            if (selectedCountry != null) {
                countryField.setText(selectedCountry);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách quốc gia: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectDate(JTextField dateField) {
        JDialog dateDialog = new JDialog(this, "Chọn ngày", true);
        dateDialog.setSize(300, 150);
        dateDialog.setLocationRelativeTo(this);
        dateDialog.setLayout(new BorderLayout());

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateDialog.add(dateSpinner, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dateDialog.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                String dateStr = sdf.format(dateSpinner.getValue());
                sdf.parse(dateStr);
                dateField.setText(dateStr);
                dateDialog.dispose();
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Ngày không hợp lệ. Vui lòng chọn lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dateDialog.dispose());

        dateDialog.setVisible(true);
    }

    private void showAddMovieDialog(JTable movieTable) {
        JDialog dialog = new JDialog(this, "Thêm phim", true);
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Tên phim:"), gbc);
        JTextField tempTitleField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        inputPanel.add(tempTitleField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Mô tả:"), gbc);
        JTextArea tempDescriptionArea = new JTextArea(3, 20);
        tempDescriptionArea.setLineWrap(true);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        inputPanel.add(new JScrollPane(tempDescriptionArea), gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Thời lượng (phút):"), gbc);
        JTextField tempDurationField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        inputPanel.add(tempDurationField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("Đạo diễn:"), gbc);
        JTextField tempDirectorField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        inputPanel.add(tempDirectorField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(new JLabel("Thể loại:"), gbc);
        JTextField tempGenreField = new JTextField(20);
        tempGenreField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 4;
        inputPanel.add(tempGenreField, gbc);
        JButton tempSelectGenreButton = new JButton("Chọn thể loại");
        gbc.gridx = 2;
        gbc.gridy = 4;
        inputPanel.add(tempSelectGenreButton, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 5;
        inputPanel.add(new JLabel("Áp phích:"), gbc);
        JTextField tempPosterField = new JTextField(20);
        tempPosterField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 5;
        inputPanel.add(tempPosterField, gbc);
        JButton tempChoosePosterButton = new JButton("Chọn hình ảnh");
        gbc.gridx = 2;
        gbc.gridy = 5;
        inputPanel.add(tempChoosePosterButton, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 6;
        inputPanel.add(new JLabel("Ngày bắt đầu (yyyy-MM-dd):"), gbc);
        JTextField tempStartDateField = new JTextField(20);
        tempStartDateField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 6;
        inputPanel.add(tempStartDateField, gbc);
        JButton tempSelectStartDateButton = new JButton("Chọn ngày");
        gbc.gridx = 2;
        gbc.gridy = 6;
        inputPanel.add(tempSelectStartDateButton, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 7;
        inputPanel.add(new JLabel("Ngày kết thúc (yyyy-MM-dd):"), gbc);
        JTextField tempEndDateField = new JTextField(20);
        tempEndDateField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 7;
        inputPanel.add(tempEndDateField, gbc);
        JButton tempSelectEndDateButton = new JButton("Chọn ngày");
        gbc.gridx = 2;
        gbc.gridy = 7;
        inputPanel.add(tempSelectEndDateButton, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 8;
        inputPanel.add(new JLabel("Năm sản xuất:"), gbc);
        JTextField tempProductionYearField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        inputPanel.add(tempProductionYearField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 9;
        inputPanel.add(new JLabel("Quốc gia:"), gbc);
        JTextField tempCountryField = new JTextField(20);
        tempCountryField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 9;
        inputPanel.add(tempCountryField, gbc);
        JButton tempSelectCountryButton = new JButton("Chọn quốc gia");
        gbc.gridx = 2;
        gbc.gridy = 9;
        inputPanel.add(tempSelectCountryButton, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 10;
        inputPanel.add(new JLabel("Giới hạn tuổi:"), gbc);
        JTextField tempAgeRestrictionField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        inputPanel.add(tempAgeRestrictionField, gbc);

        JLabel tempPosterPreview = new JLabel();
        tempPosterPreview.setPreferredSize(new Dimension(150, 200));
        tempPosterPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 11;
        inputPanel.add(tempPosterPreview, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        tempChoosePosterButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String posterPath = selectedFile.getAbsolutePath();
                tempPosterField.setText(posterPath);
                tempPosterPreview.setIcon(new ImageIcon(new ImageIcon(posterPath).getImage().getScaledInstance(150, 200, Image.SCALE_SMOOTH)));
            }
        });

        tempSelectGenreButton.addActionListener(e -> {
            try {
                List<String> genres = movieBUS.getAllGenres();
                String selectedGenre = (String) JOptionPane.showInputDialog(
                        dialog, "Chọn thể loại:", "Lựa chọn thể loại",
                        JOptionPane.PLAIN_MESSAGE, null, genres.toArray(), genres.get(0));
                if (selectedGenre != null) {
                    tempGenreField.setText(selectedGenre);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Không thể tải danh sách thể loại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        tempSelectCountryButton.addActionListener(e -> {
            try {
                List<String> countries = movieBUS.getAllCountries();
                String selectedCountry = (String) JOptionPane.showInputDialog(
                        dialog, "Chọn quốc gia:", "Lựa chọn quốc gia",
                        JOptionPane.PLAIN_MESSAGE, null, countries.toArray(), countries.get(0));
                if (selectedCountry != null) {
                    tempCountryField.setText(selectedCountry);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Không thể tải danh sách quốc gia: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        tempSelectStartDateButton.addActionListener(e -> selectDate(tempStartDateField));
        tempSelectEndDateButton.addActionListener(e -> selectDate(tempEndDateField));

        saveButton.addActionListener(e -> {
            try {
                Movie movie = new Movie();
                movie.setTitle(tempTitleField.getText().trim());
                movie.setDescription(tempDescriptionArea.getText().trim());
                movie.setDuration(Integer.parseInt(tempDurationField.getText().trim()));
                movie.setDirector(tempDirectorField.getText().trim());
                movie.setGenreName(tempGenreField.getText().trim());
                movie.setGenreID(movieDAO.getGenreIdByName(tempGenreField.getText().trim()));
                movie.setPoster(tempPosterField.getText().trim());
                movie.setStartDate(Date.valueOf(tempStartDateField.getText().trim()));
                movie.setEndDate(Date.valueOf(tempEndDateField.getText().trim()));
                movie.setProductionYear(Integer.parseInt(tempProductionYearField.getText().trim()));
                movie.setCountryName(tempCountryField.getText().trim());
                movie.setCountryID(movieDAO.getCountryIdByName(tempCountryField.getText().trim()));
                movie.setAgeRestriction(Integer.parseInt(tempAgeRestrictionField.getText().trim()));

                if (movie.getTitle().isEmpty() || movie.getGenreName().isEmpty()) {
                    throw new IllegalArgumentException("Tên phim và thể loại không được để trống");
                }

                movieBUS.addMovie(movie);
                loadMovies(movieListPanel);
                JOptionPane.showMessageDialog(dialog, "Thêm phim thành công");
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập số hợp lệ cho thời lượng, năm sản xuất và giới hạn tuổi", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, "Không thể thêm phim: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void updateMovie(JTable movieTable) {
        try {
            selectedMovie.setTitle(titleField.getText().trim());
            selectedMovie.setDescription(descriptionArea.getText().trim());
            selectedMovie.setDuration(Integer.parseInt(durationField.getText().trim()));
            selectedMovie.setDirector(directorField.getText().trim());
            selectedMovie.setGenreName(genreField.getText().trim());
            selectedMovie.setGenreID(movieDAO.getGenreIdByName(genreField.getText().trim()));
            selectedMovie.setPoster(posterField.getText().trim());
            selectedMovie.setStartDate(Date.valueOf(startDateField.getText().trim()));
            selectedMovie.setEndDate(Date.valueOf(endDateField.getText().trim()));
            selectedMovie.setProductionYear(Integer.parseInt(productionYearField.getText().trim()));
            selectedMovie.setCountryName(countryField.getText().trim());
            selectedMovie.setCountryID(movieDAO.getCountryIdByName(countryField.getText().trim()));
            selectedMovie.setAgeRestriction(Integer.parseInt(ageRestrictionField.getText().trim()));

            if (selectedMovie.getTitle().isEmpty() || selectedMovie.getGenreName().isEmpty()) {
                throw new IllegalArgumentException("Tên phim và thể loại không được để trống");
            }

            movieBUS.updateMovie(selectedMovie);
            loadMovies(movieListPanel);
            formPanel.setVisible(false);
            updateButton.setEnabled(false);
            deleteButton.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Cập nhật phim thành công");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ cho thời lượng, năm sản xuất và giới hạn tuổi", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Không thể cập nhật phim: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteMovie(JTable movieTable) {
        try {
            if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa phim này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                movieBUS.deleteMovie(selectedMovie.getMovieID());
                loadMovies(movieListPanel);
                formPanel.setVisible(false);
                updateButton.setEnabled(false);
                deleteButton.setEnabled(false);
                JOptionPane.showMessageDialog(this, "Xóa phim thành công");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể xóa phim: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    private JPanel createRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));

        // Tiêu đề
        JLabel titleLabel = new JLabel("Phòng chiếu", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Nội dung chính
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainContent.setBackground(new Color(245, 245, 245));

        // Tạo bảng
        String[] columnNames = {"ID", "Tên phòng", "Sức chứa", "Giá vé (VND)"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        roomTable = new JTable(tableModel);
        roomTable.setRowHeight(30);
        roomTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roomTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        roomTable.getTableHeader().setBackground(new Color(200, 200, 200));

        // Đặt chiều rộng cột
        roomTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        roomTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        roomTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        roomTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        // Theo dõi hàng được chọn
        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && roomTable.getSelectedRow() != -1) {
                // Hàng đã được chọn
            }
        });

        JScrollPane roomScrollPane = new JScrollPane(roomTable);
        mainContent.add(roomScrollPane, BorderLayout.CENTER);

        // Panel nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton addButton = new JButton("Thêm phòng");
        JButton editButton = new JButton("Sửa");
        JButton deleteButton = new JButton("Xóa");

        // Tùy chỉnh giao diện các nút
        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
        addButton.setFont(buttonFont);
        addButton.setForeground(Color.WHITE);
        addButton.setBackground(new Color(34, 139, 34)); // Xanh lá nhạt
        addButton.setFocusPainted(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        editButton.setFont(buttonFont);
        editButton.setForeground(Color.WHITE);
        editButton.setBackground(new Color(75, 192, 192)); // Xanh lam
        editButton.setFocusPainted(false);
        editButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        deleteButton.setFont(buttonFont);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBackground(new Color(255, 99, 71)); // Đỏ
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        mainContent.add(buttonPanel, BorderLayout.SOUTH);

        // Tải dữ liệu
        loadRooms(roomTable);

        // Xử lý sự kiện
        addButton.addActionListener(e -> showAddRoomDialog(roomTable));

        editButton.addActionListener(e -> {
            int selectedRow = roomTable.getSelectedRow();
            if (selectedRow != -1) {
                int roomID = (int) roomTable.getValueAt(selectedRow, 0);
                try {
                    List<Room> rooms = roomBUS.getAllRooms();
                    Room room = rooms.stream()
                            .filter(r -> r.getRoomID() == roomID)
                            .findFirst()
                            .orElse(null);
                    if (room != null) {
                        showEditRoomDialog(room, roomTable);
                    } else {
                        JOptionPane.showMessageDialog(this, "Không tìm thấy phòng", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng để sửa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = roomTable.getSelectedRow();
            if (selectedRow != -1) {
                int roomID = (int) roomTable.getValueAt(selectedRow, 0);
                try {
                    List<Room> rooms = roomBUS.getAllRooms();
                    Room room = rooms.stream()
                            .filter(r -> r.getRoomID() == roomID)
                            .findFirst()
                            .orElse(null);
                    if (room != null) {
                        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa phòng này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            roomBUS.deleteRoom(room.getRoomID());
                            loadRooms(roomTable);
                            JOptionPane.showMessageDialog(this, "Xóa phòng thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Không tìm thấy phòng", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng để xóa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private void loadRooms(JTable roomTable) {
        try {
            DefaultTableModel model = (DefaultTableModel) roomTable.getModel();
            model.setRowCount(0);

            List<Room> rooms = roomBUS.getAllRooms();
            if (rooms == null) {
                throw new SQLException("Danh sách phòng trả về null");
            }
            for (Room room : rooms) {
                if (room == null) continue;
                String priceStr = room.getPrice() > 0 ? String.format("%,.0f", room.getPrice()) : "Không có";
                Object[] row = {room.getRoomID(), room.getRoomName(), room.getCapacity(), priceStr};
                model.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách phòng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddRoomDialog(JTable roomTable) {
        JDialog dialog = new JDialog(this, "Thêm phòng", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(245, 245, 245));

        // Tùy chỉnh font tiêu đề dialog
        JLabel dialogTitle = new JLabel("Thêm phòng", SwingConstants.CENTER);
        dialogTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dialogTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        dialog.add(dialogTitle, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Tên phòng:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(nameLabel, gbc);
        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0;
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel capacityLabel = new JLabel("Sức chứa:");
        capacityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(capacityLabel, gbc);
        JTextField capacityField = new JTextField(20);
        capacityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 1;
        inputPanel.add(capacityField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel priceLabel = new JLabel("Giá vé (VND):");
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(priceLabel, gbc);
        JTextField priceField = new JTextField(20);
        priceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 2;
        inputPanel.add(priceField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
        saveButton.setFont(buttonFont);
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(new Color(34, 139, 34));
        saveButton.setFocusPainted(false);
        saveButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        cancelButton.setFont(buttonFont);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBackground(new Color(169, 169, 169));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String roomName = nameField.getText().trim();
                if (roomName.isEmpty()) throw new IllegalArgumentException("Tên phòng không được để trống");

                int capacity = Integer.parseInt(capacityField.getText().trim());
                if (capacity <= 0) throw new NumberFormatException("Sức chứa phải lớn hơn 0");

                double price = Double.parseDouble(priceField.getText().trim());
                if (price < 0) throw new NumberFormatException("Giá vé không được âm");

                roomBUS.addRoom(roomName, capacity, price);
                loadRooms(roomTable);
                JOptionPane.showMessageDialog(this, "Thêm phòng thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Không thể thêm phòng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showEditRoomDialog(Room room, JTable roomTable) {
        if (room == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy phòng để chỉnh sửa", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Sửa phòng", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(245, 245, 245));

        // Tùy chỉnh font tiêu đề dialog
        JLabel dialogTitle = new JLabel("Sửa phòng", SwingConstants.CENTER);
        dialogTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dialogTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        dialog.add(dialogTitle, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Tên phòng:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(nameLabel, gbc);
        JTextField nameField = new JTextField(room.getRoomName() != null ? room.getRoomName() : "", 20);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0;
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel capacityLabel = new JLabel("Sức chứa:");
        capacityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(capacityLabel, gbc);
        JTextField capacityField = new JTextField(String.valueOf(room.getCapacity()), 20);
        capacityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 1;
        inputPanel.add(capacityField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel priceLabel = new JLabel("Giá vé (VND):");
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(priceLabel, gbc);
        JTextField priceField = new JTextField(String.valueOf(room.getPrice()), 20);
        priceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 2;
        inputPanel.add(priceField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
        saveButton.setFont(buttonFont);
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(new Color(34, 139, 34));
        saveButton.setFocusPainted(false);
        saveButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        cancelButton.setFont(buttonFont);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBackground(new Color(169, 169, 169));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Tên phòng không được để trống");

                int capacity = Integer.parseInt(capacityField.getText().trim());
                if (capacity <= 0) throw new NumberFormatException("Sức chứa phải lớn hơn 0");

                double price = Double.parseDouble(priceField.getText().trim());
                if (price < 0) throw new NumberFormatException("Giá vé không được âm");

                room.setRoomName(name);
                room.setCapacity(capacity);
                room.setPrice(price);

                roomBUS.updateRoom(room);
                loadRooms(roomTable);
                JOptionPane.showMessageDialog(this, "Cập nhật phòng thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật phòng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }
    private JPanel createShowtimePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Suất chiếu", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Gán showtimeParentPanel
        showtimeParentPanel = mainContent;

        // Tạo JTable thay vì showtimeListPanel
        String[] columnNames = {"ID", "Tên phòng", "Tên phim", "Ngày chiếu", "Trạng thái"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa trực tiếp
            }
        };
        JTable showtimeTable = new JTable(tableModel);
        showtimeTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        showtimeTable.setRowHeight(30);
        showtimeTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        showtimeTable.setRowHeight(30);
        showtimeTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Tùy chỉnh kích thước cột
        showtimeTable.getColumnModel().getColumn(0).setPreferredWidth(30); // ID
        showtimeTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Tên phòng
        showtimeTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Tên phim
        showtimeTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Ngày chiếu
        showtimeTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Trạng thái

        // Đặt bảng vào JScrollPane
        JScrollPane showtimeScrollPane = new JScrollPane(showtimeTable);
        mainContent.add(showtimeScrollPane, BorderLayout.CENTER);

        // Panel chứa nút
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Thêm suất chiếu");
        JButton addMovieButton = new JButton("Thêm phim");
        JButton addStaffButton = new JButton("Thêm nhân viên");
        JButton hideButton = new JButton("Ẩn");

        // Tùy chỉnh màu sắc và font nút
        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
        addButton.setFont(buttonFont);
        addButton.setForeground(Color.WHITE);
        addButton.setBackground(new Color(75, 192, 192)); // Xanh dương nhạt
        addButton.setFocusPainted(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addButton.setEnabled(true); // Luôn sáng

        addMovieButton.setFont(buttonFont);
        addMovieButton.setForeground(Color.WHITE);
        addMovieButton.setBackground(new Color(34, 139, 34)); // Xanh lá
        addMovieButton.setFocusPainted(false);
        addMovieButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addMovieButton.setEnabled(false); // Ban đầu tắt

        addStaffButton.setFont(buttonFont);
        addStaffButton.setForeground(Color.WHITE);
        addStaffButton.setBackground(new Color(75, 192, 192)); // Xanh dương nhạt
        addStaffButton.setFocusPainted(false);
        addStaffButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        addStaffButton.setEnabled(false); // Ban đầu tắt

        hideButton.setFont(buttonFont);
        hideButton.setForeground(Color.WHITE);
        hideButton.setBackground(new Color(255, 99, 71)); // Đỏ
        hideButton.setFocusPainted(false);
        hideButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        hideButton.setEnabled(false); // Ban đầu tắt

        buttonPanel.add(addButton);
        buttonPanel.add(addMovieButton);
        buttonPanel.add(addStaffButton);
        buttonPanel.add(hideButton);
        mainContent.add(buttonPanel, BorderLayout.SOUTH);

        // Xử lý sự kiện chọn hàng trong JTable
        showtimeTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = showtimeTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && selectedRow != -1) {
                addMovieButton.setEnabled(true);
                addStaffButton.setEnabled(true);
                String status = (String) tableModel.getValueAt(selectedRow, 4);
                hideButton.setEnabled(status != null && (status.equals("Đang chiếu") || status.equals("Đã chiếu xong")));
            } else {
                addMovieButton.setEnabled(false);
                addStaffButton.setEnabled(false);
                hideButton.setEnabled(false);
            }
        });

        // Xử lý sự kiện cho các nút
        addButton.addActionListener(e -> showAddShowtimeDialog(mainContent));
        addMovieButton.addActionListener(e -> {
            int selectedRow = showtimeTable.getSelectedRow();
            if (selectedRow != -1) {
                int showtimeId = (int) tableModel.getValueAt(selectedRow, 0);
                Showtime showtime = showtimes.stream()
                        .filter(s -> s.getShowtimeID() == showtimeId)
                        .findFirst()
                        .orElse(null);
                if (showtime != null) {
                    showAddMovieToShowtimeDialog(showtime, mainContent);
                }
            }
        });
        addStaffButton.addActionListener(e -> {
            int selectedRow = showtimeTable.getSelectedRow();
            if (selectedRow != -1) {
                int showtimeId = (int) tableModel.getValueAt(selectedRow, 0);
                Showtime showtime = showtimes.stream()
                        .filter(s -> s.getShowtimeID() == showtimeId)
                        .findFirst()
                        .orElse(null);
                if (showtime != null) {
                    showAddStaffToShowtimeDialog(showtime, mainContent);
                }
            }
        });
        hideButton.addActionListener(e -> {
            int selectedRow = showtimeTable.getSelectedRow();
            if (selectedRow != -1) {
                int showtimeId = (int) tableModel.getValueAt(selectedRow, 0);
                try {
                    showtimeBUS.updateShowtimeVisibility(showtimeId, 1);
                    loadShowtimes();
                    JOptionPane.showMessageDialog(mainContent, "Ẩn suất chiếu thành công trên giao diện người dùng");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(mainContent, "Không thể ẩn suất chiếu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Load dữ liệu ban đầu
        loadShowtimes();

        // Tự động cập nhật trạng thái suất chiếu mỗi 30 giây
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> loadShowtimes());
            }
        }, 0, 30 * 1000); // Cập nhật mỗi 30 giây

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private void loadShowtimes() { // Không cần tham số JPanel parentPanel nữa
        new SwingWorker<List<Showtime>, Void>() {
            @Override
            protected List<Showtime> doInBackground() throws SQLException {
                return showtimeBUS.getAllShowtimes();
            }

            @Override
            protected void done() {
                try {
                    showtimes = get();
                    JTable showtimeTable = null;
                    for (Component comp : showtimeParentPanel.getComponents()) {
                        if (comp instanceof JScrollPane) {
                            showtimeTable = ((JScrollPane) comp).getViewport().getView() instanceof JTable
                                    ? (JTable) ((JScrollPane) comp).getViewport().getView()
                                    : null;
                            break;
                        }
                    }
                    if (showtimeTable != null) {
                        DefaultTableModel model = (DefaultTableModel) showtimeTable.getModel();
                        model.setRowCount(0);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        for (Showtime showtime : showtimes) {
                            String roomName = (showtime.getRoomName() != null) ? showtime.getRoomName() : "Phòng không xác định";
                            String movieTitle = (showtime.getMovieTitle() != null && !showtime.getMovieTitle().isEmpty())
                                    ? showtime.getMovieTitle()
                                    : "Chưa có phim";
                            String showDateStr = (showtime.getShowDate() != null)
                                    ? sdf.format(showtime.getShowDate())
                                    : "Chưa có thời gian";
                            String status = (showtime.getStatus() != null && !showtime.getStatus().isEmpty())
                                    ? showtime.getStatus()
                                    : "Không chiếu";
                            Object[] row = {showtime.getShowtimeID(), roomName, movieTitle, showDateStr, status};
                            model.addRow(row);
                        }
                    }
                    showtimeParentPanel.revalidate();
                    showtimeParentPanel.repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(showtimeParentPanel, "Không thể tải danh sách suất chiếu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    private void showAddShowtimeDialog(JPanel showtimeListPanel) {
        JDialog dialog = new JDialog(this, "Thêm suất chiếu", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Phòng:"), gbc);
        JComboBox<String> roomCombo = new JComboBox<>();
        try {
            List<Room> rooms = roomBUS.getAllRooms();
            for (Room room : rooms) {
                roomCombo.addItem(room.getRoomName());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách phòng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(roomCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Ngày giờ chiếu (yyyy-MM-dd HH:mm):"), gbc);
        JTextField showDateField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(showDateField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String roomName = (String) roomCombo.getSelectedItem();
                String showDateStr = showDateField.getText().trim();
                if (roomName == null || showDateStr.isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng chọn phòng và nhập ngày giờ chiếu");
                }
                Room selectedRoom = roomBUS.getAllRooms().stream()
                        .filter(r -> r.getRoomName().equals(roomName))
                        .findFirst()
                        .orElse(null);
                if (selectedRoom == null) {
                    throw new IllegalArgumentException("Phòng không tồn tại");
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                java.util.Date showDate = sdf.parse(showDateStr);
                Showtime showtime = new Showtime();
                showtime.setRoomID(selectedRoom.getRoomID());
                showtime.setShowDate(showDate);
                showtime.setStatus("Không chiếu");
                showtime.setMovieID(0); // Initially no movie
                showtimeDAO.addShowtime(showtime);
                loadShowtimes();
                JOptionPane.showMessageDialog(this, "Thêm suất chiếu thành công");
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể thêm suất chiếu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showAddMovieToShowtimeDialog(Showtime showtime, JPanel showtimeListPanel) {
        JDialog dialog = new JDialog(this, "Thêm phim vào suất chiếu " + showtime.getRoomName(), true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Phim:"), gbc);
        JComboBox<String> movieCombo = new JComboBox<>();
        try {
            List<Movie> movies = movieBUS.getAllMovies();
            for (Movie movie : movies) {
                movieCombo.addItem(movie.getTitle());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách phim: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(movieCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Ngày giờ chiếu (yyyy-MM-dd HH:mm):"), gbc);
        JTextField showDateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(showtime.getShowDate()), 20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(showDateField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String movieTitle = (String) movieCombo.getSelectedItem();
                String showDateStr = showDateField.getText().trim();
                if (movieTitle == null) {
                    throw new IllegalArgumentException("Vui lòng chọn phim");
                }
                Movie selectedMovie = movieBUS.getAllMovies().stream()
                        .filter(m -> m.getTitle().equals(movieTitle))
                        .findFirst()
                        .orElse(null);
                if (selectedMovie == null) {
                    throw new IllegalArgumentException("Phim không tồn tại");
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                showtime.setMovieID(selectedMovie.getMovieID());
                showtime.setShowDate(sdf.parse(showDateStr));
                showtime.setStatus("Sắp công chiếu");
                showtimeDAO.updateShowtime(showtime);
                loadShowtimes();
                JOptionPane.showMessageDialog(this, "Thêm phim vào suất chiếu thành công");
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể thêm phim: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showAddStaffToShowtimeDialog(Showtime showtime, JPanel showtimeListPanel) {
        JDialog dialog = new JDialog(this, "Thêm nhân viên vào suất chiếu " + showtime.getRoomName(), true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Nhân viên:"), gbc);
        JComboBox<String> staffCombo = new JComboBox<>();
        try {
            List<Staff> staffList = staffBUS.getAllStaff();
            for (Staff staff : staffList) {
                staffCombo.addItem(staff.getFullName());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách nhân viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(staffCombo, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String staffName = (String) staffCombo.getSelectedItem();
                if (staffName == null) {
                    throw new IllegalArgumentException("Vui lòng chọn nhân viên");
                }
                Staff selectedStaff = staffBUS.getAllStaff().stream()
                        .filter(s -> s.getFullName().equals(staffName))
                        .findFirst()
                        .orElse(null);
                if (selectedStaff == null) {
                    throw new IllegalArgumentException("Nhân viên không tồn tại");
                }
                showtime.setStaffID(selectedStaff.getStaffID());
                showtimeDAO.updateShowtime(showtime);
                loadShowtimes();
                JOptionPane.showMessageDialog(this, "Thêm nhân viên vào suất chiếu thành công");
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể thêm nhân viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }
    private JPanel createStaffPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Quản lý nhân viên", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainContent.setBackground(new Color(245, 245, 245));

        String[] columnNames = {"ID", "Tên nhân viên", "Email"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable staffTable = new JTable(tableModel);
        staffTable.setRowHeight(30);
        staffTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        staffTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        staffTable.getTableHeader().setBackground(new Color(200, 200, 200));

        staffTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        staffTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        staffTable.getColumnModel().getColumn(2).setPreferredWidth(200);

        JScrollPane staffScrollPane = new JScrollPane(staffTable);
        mainContent.add(staffScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton addButton = new JButton("Thêm nhân viên");
        JButton editButton = new JButton("Sửa");
        JButton deleteButton = new JButton("Xóa");

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 12);
        addButton.setFont(buttonFont);
        addButton.setForeground(Color.WHITE);
        addButton.setBackground(new Color(34, 139, 34)); // Xanh lá
        addButton.setFocusPainted(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        editButton.setFont(buttonFont);
        editButton.setForeground(Color.WHITE);
        editButton.setBackground(new Color(75, 192, 192)); // Xanh dương nhạt
        editButton.setFocusPainted(false);
        editButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        deleteButton.setFont(buttonFont);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBackground(new Color(255, 99, 71)); // Đỏ
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        mainContent.add(buttonPanel, BorderLayout.SOUTH);

        loadStaff(staffTable);

        addButton.addActionListener(e -> showAddStaffDialog(staffTable));
        editButton.addActionListener(e -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow != -1) {
                int staffID = (int) staffTable.getValueAt(selectedRow, 0);
                try {
                    List<Staff> staffList = staffBUS.getAllStaff();
                    Staff staff = staffList.stream()
                            .filter(s -> s.getStaffID() == staffID)
                            .findFirst()
                            .orElse(null);
                    if (staff != null) {
                        showEditStaffDialog(staff, staffTable);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên để sửa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow != -1) {
                int staffID = (int) staffTable.getValueAt(selectedRow, 0);
                try {
                    List<Staff> staffList = staffBUS.getAllStaff();
                    Staff staff = staffList.stream()
                            .filter(s -> s.getStaffID() == staffID)
                            .findFirst()
                            .orElse(null);
                    if (staff != null) {
                        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa nhân viên này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            staffBUS.deleteStaff(staff.getStaffID());
                            loadStaff(staffTable);
                            JOptionPane.showMessageDialog(this, "Xóa nhân viên thành công");
                        }
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Không thể xóa nhân viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên để xóa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private void loadStaff(JTable staffTable) {
        try {
            DefaultTableModel model = (DefaultTableModel) staffTable.getModel();
            model.setRowCount(0);
            List<Staff> staffList = staffBUS.getAllStaff();
            for (Staff staff : staffList) {
                Object[] row = {staff.getStaffID(), staff.getFullName(), staff.getEmail()};
                model.addRow(row);
            }
            staffTable.revalidate();
            staffTable.repaint();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách nhân viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddStaffDialog(JTable staffTable) {
        JDialog dialog = new JDialog(this, "Thêm nhân viên", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Tên nhân viên:"), gbc);
        JTextField nameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Email:"), gbc);
        JTextField emailField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(emailField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String fullName = nameField.getText().trim();
                String email = emailField.getText().trim();
                if (fullName.isEmpty() || email.isEmpty()) {
                    throw new IllegalArgumentException("Tên và email không được để trống");
                }
                staffBUS.addStaff(fullName, email);
                loadStaff(staffTable);
                JOptionPane.showMessageDialog(this, "Thêm nhân viên thành công");
                dialog.dispose();
            } catch (SQLException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Không thể thêm nhân viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditStaffDialog(Staff staff, JTable staffTable) {
        JDialog dialog = new JDialog(this, "Sửa nhân viên", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Tên nhân viên:"), gbc);
        JTextField nameField = new JTextField(staff.getFullName(), 20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Email:"), gbc);
        JTextField emailField = new JTextField(staff.getEmail(), 20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(emailField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                staff.setFullName(nameField.getText().trim());
                staff.setEmail(emailField.getText().trim());
                if (staff.getFullName().isEmpty() || staff.getEmail().isEmpty()) {
                    throw new IllegalArgumentException("Tên và email không được để trống");
                }
                staffBUS.updateStaff(staff);
                loadStaff(staffTable);
                JOptionPane.showMessageDialog(this, "Cập nhật nhân viên thành công");
                dialog.dispose();
            } catch (SQLException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật nhân viên: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Quản lý khách hàng", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainContent.setBackground(new Color(245, 245, 245));

        String[] columnNames = {"Tên khách hàng", "Email", "Số vé đã đặt"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable customerTable = new JTable(tableModel);
        customerTable.setRowHeight(30);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        customerTable.getTableHeader().setBackground(new Color(200, 200, 200));

        customerTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        customerTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        customerTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        JScrollPane customerScrollPane = new JScrollPane(customerTable);
        mainContent.add(customerScrollPane, BorderLayout.CENTER);

        loadCustomers(customerTable);

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private void loadCustomers(JTable customerTable) {
        try {
            DefaultTableModel model = (DefaultTableModel) customerTable.getModel();
            model.setRowCount(0);
            List<Customer> customers = customerBUS.getAllCustomers();
            for (Customer customer : customers) {
                int ticketCount = ticketBUS.getBookingHistory(customer.getCustomerID()).size();
                Object[] row = {customer.getFullName(), customer.getEmail(), ticketCount};
                model.addRow(row);
            }
            customerTable.revalidate();
            customerTable.repaint();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách khách hàng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel("Thống kê doanh thu", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel để hiển thị biểu đồ
        JPanel chartPanelContainer = new JPanel(new BorderLayout());
        mainContent.add(chartPanelContainer, BorderLayout.CENTER);

        // Nút tải thủ công
        JPanel inputPanel = new JPanel(new FlowLayout());
        JButton loadButton = new JButton("Tải thủ công");
        inputPanel.add(loadButton);
        mainContent.add(inputPanel, BorderLayout.NORTH);

        // Tự động cập nhật biểu đồ
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Tạo dataset cho biểu đồ
                        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                        Map<String, Double> dailyRevenue = revenueDAO.getDailyRevenue();
                        for (Map.Entry<String, Double> entry : dailyRevenue.entrySet()) {
                            dataset.addValue(entry.getValue(), "Doanh thu", entry.getKey());
                        }

                        // Tạo biểu đồ cột
                        JFreeChart barChart = ChartFactory.createBarChart(
                                "Thống kê doanh thu theo ngày",
                                "Ngày",
                                "Doanh thu (VND)",
                                dataset
                        );

                        // Tùy chỉnh giao diện
                        barChart.setBackgroundPaint(new Color(245, 245, 245));
                        barChart.getCategoryPlot().getRenderer().setSeriesPaint(0, new Color(75, 192, 192));

                        // Cập nhật biểu đồ
                        ChartPanel chartPanel = new ChartPanel(barChart);
                        chartPanelContainer.removeAll();
                        chartPanelContainer.add(chartPanel, BorderLayout.CENTER);
                        chartPanelContainer.revalidate();
                        chartPanelContainer.repaint();

                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(panel, "Không thể tải thống kê: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        };

        // Lên lịch chạy task mỗi 30 giây
        timer.scheduleAtFixedRate(task, 0, 30000);

        // Nút tải thủ công
        loadButton.addActionListener(e -> task.run());

        // Tự động tải lần đầu
        task.run();

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }
}