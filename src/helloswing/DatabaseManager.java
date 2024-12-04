package helloswing;

import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class DatabaseManager extends javax.swing.JFrame {
    private Connection connection;
    private boolean isLogin;
    private String currentDatabase = null;
    private ActionListener tableComboListener;
    private TreeSelectionListener treeSelectionListener;
    private final DatabaseOperations dbOps;
    private TableOperations tableOps;
    private StatusLogger statusLogger;

    public DatabaseManager(boolean isLogin, Connection connection) {
        this.isLogin = isLogin;
        this.connection = connection;
        this.dbOps = new DatabaseOperations(connection);
        this.tableOps = new TableOperations(connection);
        if (!this.isLogin) {
            new SQLLogin().setVisible(true);
            this.dispose();
        } else {
            initComponents();
            this.statusLogger = new StatusLogger(txtpnStatus);
            loadDatabases();
            loadDatabaseTree();
            setLocationRelativeTo(null);
            addTreeSelectionListener();
            statusLogger.logSuccess("Connected to MySQL server successfully!");
        }
    }

    public void loadDatabases() {
        try {
            List<String> databases = dbOps.getDatabases();
            comboboxDB.removeAllItems();
            databases.stream()
                    .filter(dbName -> !DatabaseConstants.SYSTEM_DATABASES.contains(dbName))
                    .forEach(comboboxDB::addItem);
            statusLogger.logSuccess("Loaded database list successfully");            
            this.toFront();
            pack();
        } catch (SQLException e) {
            statusLogger.logError("Failed to load databases: " + e.getMessage());
            DialogUtils.showErrorDialog(this, DatabaseConstants.ERROR_CONNECTION, "Error");
        }
    }

    private void loadTables(String database) {
        if (tableComboListener != null) {
            comboboxTableDB.removeActionListener(tableComboListener);
            tableComboListener = null;
        }
        comboboxTableDB.removeAllItems();
        if (database == null || database.trim().isEmpty()) {
            statusLogger.logError(DatabaseConstants.ERROR_NO_DATABASE);
            return;
        }
        try {
            List<String> tables = dbOps.getTables(database);
            tables.forEach(comboboxTableDB::addItem);
            statusLogger.logSuccess("Loaded tables for database: " + database);
        } catch (SQLException e) {
            statusLogger.logError("Failed to load tables: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Failed to load tables: " + e.getMessage(), "Error");
        }
    }

    private List<String> getTables(String database) throws SQLException {
        if (database == null || database.trim().isEmpty()) {
            statusLogger.logError(DatabaseConstants.ERROR_NO_DATABASE);
            return Collections.emptyList();
        }
        try {
            List<String> tables = dbOps.getTables(database);
            statusLogger.logSuccess("Loaded tables for database: " + database);
            return tables;
        } catch (SQLException e) {
            statusLogger.logError("Failed to load tables: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Failed to load tables: " + e.getMessage(), "Error");
            throw e;
        }
    }

    private void loadTableData(String tableName) {
        if (currentDatabase == null) {
            statusLogger.logError("Please select a database first");
            return;
        }
        try {
            ResultSet rs = dbOps.getTableData(connection, currentDatabase, tableName);
            try {
                DefaultTableModel model = tableOps.createTableModel(rs);
                tblDB.setModel(model);
                tblDB.setFont(new java.awt.Font("Segoe UI", 0, 18));
                tblDB.setRowHeight(30);
                tblDB.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
                statusLogger.logSuccess("Successfully loaded " + tableName + " of " + currentDatabase + "!");
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            statusLogger.logError("Error loading table: " + tableName + "\nError: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Failed to load table " + tableName + " of " + currentDatabase + ": " + e.getMessage(), "Error");
        }
    }

    private void loadDatabaseTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Databases");
        try {
            List<String> databases = dbOps.getDatabases();
            for (String dbName : databases) {
                if (!DatabaseConstants.SYSTEM_DATABASES.contains(dbName)) {
                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(dbName);
                    root.add(dbNode);
                    List<String> tables = dbOps.getTables(dbName);
                    for (String tableName : tables) {
                        dbNode.add(new DefaultMutableTreeNode(tableName));
                    }
                }
            }
        } catch (SQLException e) {
            DialogUtils.showErrorDialog(this, "Failed to load database tree: " + e.getMessage(), "Error");
        }
        DBTree.setModel(new DefaultTreeModel(root));
    }

    private void handleDatabaseSelection(String database) {
        try {
            if (database != null && !database.isEmpty()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("USE " + database);
                    currentDatabase = database;
                    loadTables(database);
                    statusLogger.logSuccess("Database selected: " + database);
                }
            }
        } catch (SQLException e) {
            statusLogger.logError("Error selecting database: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Error selecting database: " + e.getMessage(), "Error");
        }
    }

    private void addTreeSelectionListener() {
        treeSelectionListener = e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) DBTree.getLastSelectedPathComponent();
            if (selectedNode == null) return;
            if (selectedNode.getLevel() == 1) {
                String selectedDatabase = selectedNode.getUserObject().toString();
                handleDatabaseSelection(selectedDatabase);
            } else if (selectedNode.getLevel() == 2) {
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
                String selectedDatabase = parentNode.getUserObject().toString();
                handleDatabaseSelection(selectedDatabase);
                String selectedTable = selectedNode.getUserObject().toString();
                loadTableData(selectedTable);
            }
        };
        DBTree.addTreeSelectionListener(treeSelectionListener);
    }

    private void removeTreeSelectionListener() {
        if (treeSelectionListener != null) {
            DBTree.removeTreeSelectionListener(treeSelectionListener);
            treeSelectionListener = null;
        }
    }
    
    public void refreshComboBoxes() {
        loadDatabases();
        String selectedDatabase = (String) comboboxDB.getSelectedItem();
        if (selectedDatabase != null) {
            loadTables(selectedDatabase);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnMain = new javax.swing.JPanel();
        pnFile = new javax.swing.JPanel();
        btnShow = new javax.swing.JButton();
        btnNew = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        pnCommand = new javax.swing.JPanel();
        btnSelect = new javax.swing.JButton();
        btnInsert = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnCreate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnDrop = new javax.swing.JButton();
        pnStatus = new javax.swing.JPanel();
        btnClear = new javax.swing.JButton();
        sclpnStatus = new javax.swing.JScrollPane();
        txtpnStatus = new javax.swing.JTextPane();
        pnCurrentDB = new javax.swing.JPanel();
        comboboxDB = new javax.swing.JComboBox<>();
        lblCurrentDB = new javax.swing.JLabel();
        comboboxTableDB = new javax.swing.JComboBox<>();
        btnChooseDB = new javax.swing.JButton();
        btnRefreshCombobox = new javax.swing.JButton();
        pnTable = new javax.swing.JPanel();
        sclpnTableDB = new javax.swing.JScrollPane();
        tblDB = new javax.swing.JTable();
        pnTree = new javax.swing.JPanel();
        SclpnTree = new javax.swing.JScrollPane();
        DBTree = new javax.swing.JTree();
        btnRefreshTree = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Database Manager");
        setResizable(false);

        pnMain.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        pnMain.setPreferredSize(new java.awt.Dimension(1251, 748));

        pnFile.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "File"));
        pnFile.setLayout(new java.awt.GridLayout(1, 0));

        btnShow.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnShow.setText("SHOW");
        btnShow.setToolTipText("Show database tables");
        btnShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowActionPerformed(evt);
            }
        });
        pnFile.add(btnShow);

        btnNew.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnNew.setText("NEW");
        btnNew.setToolTipText("Create new database");
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        pnFile.add(btnNew);

        btnClose.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnClose.setText("CLOSE");
        btnClose.setToolTipText("Close and Switch back to default database");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        pnFile.add(btnClose);

        pnCommand.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Commands"));
        pnCommand.setPreferredSize(new java.awt.Dimension(240, 120));
        pnCommand.setLayout(new java.awt.GridLayout(2, 0));

        btnSelect.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnSelect.setText("SELECT");
        btnSelect.setToolTipText("Get data from one or more tables");
        btnSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectActionPerformed(evt);
            }
        });
        pnCommand.add(btnSelect);

        btnInsert.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnInsert.setText("INSERT");
        btnInsert.setToolTipText("Insert new data into a table");
        btnInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInsertActionPerformed(evt);
            }
        });
        pnCommand.add(btnInsert);

        btnUpdate.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnUpdate.setText("UPDATE");
        btnUpdate.setToolTipText("Update existing data in the table");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        pnCommand.add(btnUpdate);

        btnCreate.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnCreate.setText("CREATE");
        btnCreate.setToolTipText("Create a new table");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });
        pnCommand.add(btnCreate);

        btnDelete.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnDelete.setText("DELETE");
        btnDelete.setToolTipText("Delete data from table");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        pnCommand.add(btnDelete);

        btnDrop.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnDrop.setText("DROP");
        btnDrop.setToolTipText("Completely delete a table from the database");
        btnDrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDropActionPerformed(evt);
            }
        });
        pnCommand.add(btnDrop);

        pnStatus.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Status"));

        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        txtpnStatus.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        sclpnStatus.setViewportView(txtpnStatus);

        javax.swing.GroupLayout pnStatusLayout = new javax.swing.GroupLayout(pnStatus);
        pnStatus.setLayout(pnStatusLayout);
        pnStatusLayout.setHorizontalGroup(
            pnStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(btnClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(sclpnStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        pnStatusLayout.setVerticalGroup(
            pnStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnStatusLayout.createSequentialGroup()
                .addComponent(sclpnStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 483, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClear))
        );

        pnCurrentDB.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pnCurrentDB.setPreferredSize(new java.awt.Dimension(4, 36));

        comboboxDB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboboxDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboboxDBActionPerformed(evt);
            }
        });

        lblCurrentDB.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblCurrentDB.setText("Current Database");

        comboboxTableDB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboboxTableDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboboxTableDBActionPerformed(evt);
            }
        });

        btnChooseDB.setText("Choose");
        btnChooseDB.setToolTipText("Choose this table of this database");
        btnChooseDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChooseDBActionPerformed(evt);
            }
        });

        btnRefreshCombobox.setText("Refresh");
        btnRefreshCombobox.setToolTipText("Refresh combo box");
        btnRefreshCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshComboboxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnCurrentDBLayout = new javax.swing.GroupLayout(pnCurrentDB);
        pnCurrentDB.setLayout(pnCurrentDBLayout);
        pnCurrentDBLayout.setHorizontalGroup(
            pnCurrentDBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnCurrentDBLayout.createSequentialGroup()
                .addComponent(lblCurrentDB, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboboxDB, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboboxTableDB, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnChooseDB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRefreshCombobox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnCurrentDBLayout.setVerticalGroup(
            pnCurrentDBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnCurrentDBLayout.createSequentialGroup()
                .addComponent(lblCurrentDB)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(pnCurrentDBLayout.createSequentialGroup()
                .addGroup(pnCurrentDBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboboxDB)
                    .addComponent(comboboxTableDB)
                    .addComponent(btnChooseDB)
                    .addComponent(btnRefreshCombobox))
                .addContainerGap())
        );

        pnTable.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Table", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        tblDB.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        tblDB.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        tblDB.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        sclpnTableDB.setViewportView(tblDB);

        javax.swing.GroupLayout pnTableLayout = new javax.swing.GroupLayout(pnTable);
        pnTable.setLayout(pnTableLayout);
        pnTableLayout.setHorizontalGroup(
            pnTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnTableLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(sclpnTableDB, javax.swing.GroupLayout.PREFERRED_SIZE, 770, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnTableLayout.setVerticalGroup(
            pnTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnTableLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(sclpnTableDB, javax.swing.GroupLayout.PREFERRED_SIZE, 670, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pnTree.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Database Tree"));

        SclpnTree.setViewportView(DBTree);

        btnRefreshTree.setText("Refresh");
        btnRefreshTree.setToolTipText("Refresh tree");
        btnRefreshTree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshTreeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnTreeLayout = new javax.swing.GroupLayout(pnTree);
        pnTree.setLayout(pnTreeLayout);
        pnTreeLayout.setHorizontalGroup(
            pnTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnTreeLayout.createSequentialGroup()
                .addGroup(pnTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SclpnTree, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                    .addComponent(btnRefreshTree, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnTreeLayout.setVerticalGroup(
            pnTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnTreeLayout.createSequentialGroup()
                .addComponent(SclpnTree, javax.swing.GroupLayout.PREFERRED_SIZE, 669, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnRefreshTree))
        );

        javax.swing.GroupLayout pnMainLayout = new javax.swing.GroupLayout(pnMain);
        pnMain.setLayout(pnMainLayout);
        pnMainLayout.setHorizontalGroup(
            pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnMainLayout.createSequentialGroup()
                .addGroup(pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnCommand, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnTree, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(pnCurrentDB, javax.swing.GroupLayout.DEFAULT_SIZE, 1288, Short.MAX_VALUE)
        );
        pnMainLayout.setVerticalGroup(
            pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnMainLayout.createSequentialGroup()
                .addGroup(pnMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnMainLayout.createSequentialGroup()
                        .addComponent(pnFile, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnCommand, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(pnTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnMainLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pnTree, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnCurrentDB, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1292, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnMain, javax.swing.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRefreshTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshTreeActionPerformed
        loadDatabases();
        addTreeSelectionListener(); 
        loadDatabaseTree();
        removeTreeSelectionListener();
    }//GEN-LAST:event_btnRefreshTreeActionPerformed

    private void btnRefreshComboboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshComboboxActionPerformed
        refreshComboBoxes();
    }//GEN-LAST:event_btnRefreshComboboxActionPerformed

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
        String dbName = DialogUtils.showInputDialog(this, DatabaseConstants.PROMPT_NEW_DB, "New Database");
        if (dbName != null && !dbName.trim().isEmpty()) {
            try {
                if (!dbName.matches("^[a-zA-Z0-9_]+$")) {
                    DialogUtils.showErrorDialog(this, DatabaseConstants.ERROR_INVALID_NAME, "Invalid Name");
                    return;
                }
                DatabaseOperations dbOps = new DatabaseOperations(connection);
                dbOps.createDatabase(dbName);
                loadDatabases();
                statusLogger.logSuccess(String.format(DatabaseConstants.SUCCESS_CREATE, dbName));
                DialogUtils.showInfoDialog(this, String.format(DatabaseConstants.SUCCESS_CREATE, dbName), "Success");
            } catch (SQLException e) {
                statusLogger.logError(String.format(DatabaseConstants.ERROR_CONNECTION, e.getMessage()));
                DialogUtils.showErrorDialog(this, String.format(DatabaseConstants.ERROR_CONNECTION, e.getMessage()), "Error");
            }
        }
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnUpdateActionPerformed
        if (currentDatabase == null) {
            DialogUtils.showErrorDialog(this, DatabaseConstants.ERROR_NO_DATABASE, "No Database");
            return;
        }
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            DialogUtils.showErrorDialog(this, DatabaseConstants.ERROR_NO_TABLE, "No Table");
            return;
        }
        int selectedRow = tblDB.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtils.showErrorDialog(this, DatabaseConstants.ERROR_NO_SELECTION, "No Selection");
            return;
        }
        try {
            tableOps.updateRecordWithPrompt(connection, currentDatabase, selectedTable, tblDB, selectedRow, this);
            loadTableData(selectedTable);
            statusLogger.logSuccess(String.format(DatabaseConstants.SUCCESS_UPDATE, selectedTable));
            statusLogger.logSuccess("Updated record in " + selectedTable);
            DialogUtils.showInfoDialog(this, "Record updated successfully!", "Success");
        } catch (SQLException e) {
            statusLogger.logError(String.format(DatabaseConstants.ERROR_UPDATE, e.getMessage()));
            statusLogger.logError("Error updating record: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Error updating record: " + e.getMessage(), "Update Error");
        }
    }// GEN-LAST:event_btnUpdateActionPerformed

    private void btnShowActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnShowActionPerformed
        String selectedDB = (String) comboboxDB.getSelectedItem();
        if (selectedDB != null) {
            try {
                DatabaseOperations dbOps = new DatabaseOperations(connection);
                StyledDocument doc = txtpnStatus.getStyledDocument();
                dbOps.showTables(selectedDB, doc);
                btnShow.setEnabled(false);
                btnClose.setEnabled(true);
            } catch (SQLException | BadLocationException e) {
                DialogUtils.showErrorDialog(this, "Error opening database: " + e.getMessage(), "Error");
            }
        }
    }// GEN-LAST:event_btnShowActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCloseActionPerformed
        try {
            DatabaseOperations dbOps = new DatabaseOperations(connection);
            StyledDocument doc = txtpnStatus.getStyledDocument();
            dbOps.closeDatabase(doc);
            btnShow.setEnabled(true);
            btnClose.setEnabled(false);
        } catch (SQLException | BadLocationException e) {
            DialogUtils.showErrorDialog(this, "Error closing database: " + e.getMessage(), "Error");
        }
    }// GEN-LAST:event_btnCloseActionPerformed

    private void btnSelectActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSelectActionPerformed
        if (currentDatabase == null) {
            DialogUtils.showErrorDialog(this, "Please select a database first", "No Database");
            return;
        }
        List<String> tables;
        try {
            tables = getTables(currentDatabase);
        } catch (SQLException e) {
            return;
        }
        if (tables.isEmpty()) {
            DialogUtils.showErrorDialog(this, "No tables found in the database", "No Tables");
            return;
        }
        Map<String, Object> tableAndColumns = DialogUtils.showTableAndColumnsDialog(this, tables.toArray(new String[0]));
        if (tableAndColumns.isEmpty()) {
            return;
        }
        String table = (String) tableAndColumns.get("table");
        String columns = (String) tableAndColumns.get("columns");
        if (table != null && !table.trim().isEmpty() && columns != null && !columns.trim().isEmpty()) {
            try {
                String query = "SELECT " + columns + " FROM " + QueryBuilder.escapeTableName(table);
                DatabaseOperations dbOps = new DatabaseOperations(connection);
                DefaultTableModel model = dbOps.executeSelectQuery(query);
                tblDB.setModel(model);
                statusLogger.log("Executed query: " + query);
            } catch (SQLException e) {
                statusLogger.logError("Error executing query: " + e.getMessage());
                DialogUtils.showErrorDialog(this, "Error executing query: " + e.getMessage(), "Query Error");
            }
        }
    }// GEN-LAST:event_btnSelectActionPerformed

    private void btnInsertActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnInsertActionPerformed
        if (currentDatabase == null) {
            DialogUtils.showErrorDialog(this, "Please select a database first", "No Database");
            return;
        }
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            DialogUtils.showErrorDialog(this, "Please select a table first", "No Table");
            return;
        }
        try {
            tableOps.insertRecordWithPrompt(currentDatabase, selectedTable, this);
            loadTableData(selectedTable);
            statusLogger.logSuccess("Inserted new record into " + selectedTable);
            DialogUtils.showInfoDialog(this, "Record inserted successfully!", "Success");
        } catch (SQLException e) {
            statusLogger.logError("Error inserting record: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Error inserting record: " + e.getMessage(), "Insert Error");
        }
    }// GEN-LAST:event_btnInsertActionPerformed

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCreateActionPerformed
        if (currentDatabase == null) {
            DialogUtils.showErrorDialog(this, "Please select a database first", "No Database");
            return;
        }
        String tableName = DialogUtils.showInputDialog(this, "Enter new table name:", "New Table");
        if (tableName == null || tableName.trim().isEmpty()) {
            DialogUtils.showErrorDialog(this, "Table name cannot be empty", "Invalid Name");
            return;
        }
        String columns = DialogUtils.showColumnInputDialog(this);
        if (columns == null || columns.trim().isEmpty()) {
            DialogUtils.showErrorDialog(this, "Columns cannot be empty", "Invalid Columns");
            return;
        }
        try {
            TableOperations tableOps = new TableOperations(connection);
            tableOps.createTable(currentDatabase, tableName, columns);
            loadTables(currentDatabase);    
            statusLogger.logSuccess("Created new table: " + tableName);
            DialogUtils.showInfoDialog(this, "Table '" + tableName + "' created successfully!", "Success");
        } catch (SQLException e) {
            statusLogger.logError("Error creating table: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Error creating table: " + e.getMessage(), "Error");
        }
    }// GEN-LAST:event_btnCreateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDeleteActionPerformed
        if (currentDatabase == null) {
            DialogUtils.showErrorDialog(this, "Please select a database first", "No Database");
            return;
        }
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            DialogUtils.showErrorDialog(this, "Please select a table first", "No Table");
            return;
        }
        int selectedRow = tblDB.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtils.showErrorDialog(this, "Please select a row to delete", "No Selection");
            return;
        }
        try {
            tableOps.deleteRecordWithPrompt(currentDatabase, selectedTable, tblDB, selectedRow, this);
            loadTableData(selectedTable);
            statusLogger.logSuccess("Deleted record from " + selectedTable);
            DialogUtils.showInfoDialog(this, "Record deleted successfully!", "Success");
        } catch (SQLException e) {
            statusLogger.logError("Error deleting record: " + e.getMessage());
            DialogUtils.showErrorDialog(this, "Error deleting record: " + e.getMessage(), "Delete Error");
        }
    }// GEN-LAST:event_btnDeleteActionPerformed

    private void btnDropActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnDropActionPerformed
        if (currentDatabase == null) {
            DialogUtils.showErrorDialog(this, "Please select a database first", "No Database");
            return;
        }
        String selectedTable = (String) comboboxTableDB.getSelectedItem();
        if (selectedTable == null) {
            DialogUtils.showErrorDialog(this, "Please select a table first", "No Table");
            return;
        }
        int confirm = DialogUtils.showConfirmDialog(this, "Are you sure you want to drop the table: " + selectedTable + "?", "Confirm Drop");
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                tableOps.dropTable(currentDatabase, selectedTable);
                loadTables(currentDatabase);
                statusLogger.logSuccess("Dropped table: " + selectedTable);
                DialogUtils.showInfoDialog(this, "Table dropped successfully!", "Success");
            } catch (SQLException e) {
                statusLogger.logError("Error dropping table: " + e.getMessage());
                DialogUtils.showErrorDialog(this, "Error dropping table: " + e.getMessage(), "Drop Error");
            }
        }
    }// GEN-LAST:event_btnDropActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnClearActionPerformed
        txtpnStatus.setText("");
    }// GEN-LAST:event_btnClearActionPerformed

    private void comboboxDBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboboxDBActionPerformed        
        String selectedDatabase = (String) comboboxDB.getSelectedItem();
        if (comboboxTableDB.getSelectedItem() != null) {
            comboboxTableDB.setSelectedItem(null);
        }
        handleDatabaseSelection(selectedDatabase);
    }// GEN-LAST:event_comboboxDBActionPerformed

    private void comboboxTableDBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_comboboxTableDBActionPerformed
        if (currentDatabase != null && evt.getSource() == comboboxTableDB) {
            String selectedTable = (String) comboboxTableDB.getSelectedItem();
            if (selectedTable != null) {
                statusLogger.log("Table selected: " + selectedTable);
            }
        }
    }// GEN-LAST:event_comboboxTableDBActionPerformed

    private void btnChooseDBActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnChooseDBActionPerformed
        if (currentDatabase != null) {
            String selectedTable = (String) comboboxTableDB.getSelectedItem();
            if (selectedTable != null) {
                loadTableData(selectedTable);
            }
        }
    }// GEN-LAST:event_btnChooseDBActionPerformed

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DatabaseManager.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        }

        SQLLogin login = new SQLLogin();
        login.setVisible(true);

        if (login.isLoginIn()) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new DatabaseManager(false, null);
                }
            });
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree DBTree;
    private javax.swing.JScrollPane SclpnTree;
    private javax.swing.JButton btnChooseDB;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnCreate;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnDrop;
    private javax.swing.JButton btnInsert;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnRefreshCombobox;
    private javax.swing.JButton btnRefreshTree;
    private javax.swing.JButton btnSelect;
    private javax.swing.JButton btnShow;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> comboboxDB;
    private javax.swing.JComboBox<String> comboboxTableDB;
    private javax.swing.JLabel lblCurrentDB;
    private javax.swing.JPanel pnCommand;
    private javax.swing.JPanel pnCurrentDB;
    private javax.swing.JPanel pnFile;
    private javax.swing.JPanel pnMain;
    private javax.swing.JPanel pnStatus;
    private javax.swing.JPanel pnTable;
    private javax.swing.JPanel pnTree;
    private javax.swing.JScrollPane sclpnStatus;
    private javax.swing.JScrollPane sclpnTableDB;
    private javax.swing.JTable tblDB;
    private javax.swing.JTextPane txtpnStatus;
    // End of variables declaration//GEN-END:variables
}